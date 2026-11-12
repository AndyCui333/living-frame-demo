package com.livingframe.system.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.livingframe.common.api.PageResult;
import com.livingframe.common.api.ResultCode;
import com.livingframe.common.constant.Constants;
import com.livingframe.common.context.LoginUserContext;
import com.livingframe.common.event.UserCreatedEvent;
import com.livingframe.common.event.UserDeletedEvent;
import com.livingframe.common.exception.BusinessException;
import com.livingframe.system.dto.AssignRolesRequest;
import com.livingframe.system.dto.UserCreateRequest;
import com.livingframe.system.dto.UserQuery;
import com.livingframe.system.dto.UserUpdateRequest;
import com.livingframe.system.entity.SysRole;
import com.livingframe.system.entity.SysUser;
import com.livingframe.system.entity.SysUserRole;
import com.livingframe.system.mapper.SysRoleMapper;
import com.livingframe.system.mapper.SysUserMapper;
import com.livingframe.system.mapper.SysUserRoleMapper;
import com.livingframe.system.vo.RoleBriefVO;
import com.livingframe.system.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 后台用户服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final RedissonClient redissonClient;
    private final ApplicationEventPublisher eventPublisher;

    public PageResult<UserVO> page(UserQuery query) {
        query.normalize();
        LambdaQueryWrapper<SysUser> wrapper = Wrappers.lambdaQuery();
        if (StrUtil.isNotBlank(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(SysUser::getUsername, keyword).or().like(SysUser::getNickname, keyword));
        }
        if (query.getStatus() != null) {
            wrapper.eq(SysUser::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(SysUser::getId);
        Page<SysUser> page = userMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        List<UserVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    public UserVO get(Long id) {
        SysUser user = requireUser(id);
        return toVO(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(UserCreateRequest request) {
        String username = request.username().trim();
        validateRoleIds(request.roleIds());
        assertCanAssignSuperAdmin(request.roleIds());
        RLock lock = redissonClient.getLock("living-frame:lock:sys-user:" + username);
        boolean locked = false;
        try {
            locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!locked) {
                throw new BusinessException("操作过于频繁，请稍后重试");
            }
            long exists = userMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                    .eq(SysUser::getUsername, username));
            if (exists > 0) {
                throw new BusinessException(ResultCode.CONFLICT, "用户名已存在");
            }
            SysUser user = new SysUser()
                    .setUsername(username)
                    .setPassword(passwordEncoder.encode(request.password()))
                    .setNickname(StrUtil.blankToDefault(request.nickname(), username))
                    .setEmail(request.email())
                    .setPhone(request.phone())
                    .setAvatar(request.avatar())
                    .setStatus(Constants.STATUS_ENABLED);
            userMapper.insert(user);
            replaceRoles(user.getId(), request.roleIds());
            eventPublisher.publishEvent(new UserCreatedEvent(user.getId()));
            return user.getId();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException("创建用户被中断，请重试");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, UserUpdateRequest request) {
        SysUser user = requireUser(id);
        user.setNickname(request.nickname());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setAvatar(request.avatar());
        userMapper.updateById(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysUser user = requireUser(id);
        Long currentId = LoginUserContext.require().getId();
        if (Objects.equals(currentId, id)) {
            throw new BusinessException("不能删除当前登录用户");
        }
        if (isSuperAdmin(id)) {
            throw new BusinessException("不能删除超级管理员");
        }
        userRoleMapper.deleteByUserId(id);
        userMapper.deleteById(user.getId());
        eventPublisher.publishEvent(new UserDeletedEvent(id));
        kickout(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        SysUser user = requireUser(id);
        if (Objects.equals(status, Constants.STATUS_DISABLED)) {
            if (Objects.equals(LoginUserContext.require().getId(), id)) {
                throw new BusinessException("不能禁用当前登录用户");
            }
            if (isSuperAdmin(id)) {
                throw new BusinessException("不能禁用超级管理员");
            }
        }
        user.setStatus(status);
        userMapper.updateById(user);
        if (Objects.equals(status, Constants.STATUS_DISABLED)) {
            kickout(id);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id, String password) {
        SysUser user = requireUser(id);
        if (isSuperAdmin(id) && !LoginUserContext.require().isSuperAdmin()) {
            throw new BusinessException("只有超级管理员可以重置超级管理员密码");
        }
        user.setPassword(passwordEncoder.encode(password));
        userMapper.updateById(user);
        kickout(id);
    }

    public List<RoleBriefVO> getRoles(Long id) {
        requireUser(id);
        return roleMapper.selectByUserId(id).stream()
                .map(role -> new RoleBriefVO(role.getId(), role.getCode(), role.getName()))
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long id, AssignRolesRequest request) {
        requireUser(id);
        if (isSuperAdmin(id) && !LoginUserContext.require().isSuperAdmin()) {
            throw new BusinessException("只有超级管理员可以修改超级管理员的角色");
        }
        validateRoleIds(request.roleIds());
        assertCanAssignSuperAdmin(request.roleIds());
        replaceRoles(id, request.roleIds());
        kickout(id);
    }

    public List<SysRole> selectRolesByUserId(Long userId) {
        return roleMapper.selectByUserId(userId);
    }

    private void replaceRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.deleteByUserId(userId);
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (Long roleId : roleIds.stream().filter(Objects::nonNull).distinct().toList()) {
            SysUserRole relation = new SysUserRole();
            relation.setUserId(userId);
            relation.setRoleId(roleId);
            relation.setCreatedAt(now);
            userRoleMapper.insert(relation);
        }
    }

    private void validateRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        List<Long> distinctIds = roleIds.stream().filter(Objects::nonNull).distinct().toList();
        if (distinctIds.isEmpty()) {
            return;
        }
        List<SysRole> roles = roleMapper.selectBatchIds(distinctIds);
        if (roles.size() != distinctIds.size()) {
            throw new BusinessException("存在无效的角色");
        }
    }

    /**
     * 防止系统管理员通过创建用户或分配角色给自己/他人提权到超级管理员。
     */
    private void assertCanAssignSuperAdmin(List<Long> roleIds) {
        if (LoginUserContext.require().isSuperAdmin() || roleIds == null || roleIds.isEmpty()) {
            return;
        }
        List<Long> distinctIds = roleIds.stream().filter(Objects::nonNull).distinct().toList();
        if (distinctIds.isEmpty()) {
            return;
        }
        boolean containsSuperAdmin = roleMapper.selectBatchIds(distinctIds).stream()
                .anyMatch(role -> Constants.ROLE_SUPER_ADMIN.equals(role.getCode()));
        if (containsSuperAdmin) {
            throw new BusinessException("只有超级管理员可以分配超级管理员角色");
        }
    }

    private SysUser requireUser(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private boolean isSuperAdmin(Long userId) {
        return roleMapper.selectCodesByUserId(userId).contains(Constants.ROLE_SUPER_ADMIN);
    }

    private void kickout(Long userId) {
        try {
            StpUtil.kickout(Constants.ADMIN_LOGIN_ID_PREFIX + userId);
        } catch (Exception ex) {
            log.debug("踢出用户 {} 失败: {}", userId, ex.getMessage());
        }
    }

    private UserVO toVO(SysUser user) {
        List<RoleBriefVO> roles = roleMapper.selectByUserId(user.getId()).stream()
                .map(role -> new RoleBriefVO(role.getId(), role.getCode(), role.getName()))
                .toList();
        return new UserVO(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                user.getPhone(),
                user.getAvatar(),
                user.getStatus(),
                user.getLastLoginAt() == null ? null : DATE_TIME_FORMATTER.format(user.getLastLoginAt()),
                user.getCreatedAt() == null ? null : DATE_TIME_FORMATTER.format(user.getCreatedAt()),
                roles);
    }
}
