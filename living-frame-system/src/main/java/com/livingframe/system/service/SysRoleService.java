package com.livingframe.system.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.livingframe.common.api.PageResult;
import com.livingframe.common.api.ResultCode;
import com.livingframe.common.constant.Constants;
import com.livingframe.common.exception.BusinessException;
import com.livingframe.system.dto.AssignPermissionsRequest;
import com.livingframe.system.dto.RoleCreateRequest;
import com.livingframe.system.dto.RoleQuery;
import com.livingframe.system.dto.RoleUpdateRequest;
import com.livingframe.system.entity.SysPermission;
import com.livingframe.system.entity.SysRole;
import com.livingframe.system.entity.SysRolePermission;
import com.livingframe.system.entity.SysUserRole;
import com.livingframe.system.mapper.SysPermissionMapper;
import com.livingframe.system.mapper.SysRoleMapper;
import com.livingframe.system.mapper.SysRolePermissionMapper;
import com.livingframe.system.mapper.SysUserRoleMapper;
import com.livingframe.system.vo.RoleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/**
 * 角色服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysRoleService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SysRoleMapper roleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysPermissionMapper permissionMapper;

    public PageResult<RoleVO> page(RoleQuery query) {
        query.normalize();
        LambdaQueryWrapper<SysRole> wrapper = Wrappers.lambdaQuery();
        if (StrUtil.isNotBlank(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(SysRole::getCode, keyword).or().like(SysRole::getName, keyword));
        }
        if (query.getStatus() != null) {
            wrapper.eq(SysRole::getStatus, query.getStatus());
        }
        wrapper.orderByAsc(SysRole::getSort).orderByAsc(SysRole::getId);
        Page<SysRole> page = roleMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        List<RoleVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    public List<RoleVO> listAll() {
        return roleMapper.selectList(Wrappers.<SysRole>lambdaQuery()
                        .orderByAsc(SysRole::getSort).orderByAsc(SysRole::getId))
                .stream().map(this::toVO).toList();
    }

    public RoleVO get(Long id) {
        return toVO(requireRole(id));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(RoleCreateRequest request) {
        String code = request.code().trim();
        long exists = roleMapper.selectCount(Wrappers.<SysRole>lambdaQuery().eq(SysRole::getCode, code));
        if (exists > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "角色编码已存在");
        }
        SysRole role = new SysRole()
                .setCode(code)
                .setName(request.name())
                .setDescription(request.description())
                .setSort(request.sort() == null ? 0 : request.sort())
                .setStatus(Constants.STATUS_ENABLED);
        roleMapper.insert(role);
        return role.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, RoleUpdateRequest request) {
        SysRole role = requireRole(id);
        if (Constants.ROLE_SUPER_ADMIN.equals(role.getCode())
                && request.status() != null
                && request.status() == Constants.STATUS_DISABLED) {
            throw new BusinessException("超级管理员角色不能禁用");
        }
        role.setName(request.name());
        role.setDescription(request.description());
        if (request.sort() != null) {
            role.setSort(request.sort());
        }
        if (request.status() != null) {
            role.setStatus(request.status());
        }
        roleMapper.updateById(role);
        if (request.status() != null && request.status() == Constants.STATUS_DISABLED) {
            kickoutUsers(id);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysRole role = requireRole(id);
        if (Constants.ROLE_SUPER_ADMIN.equals(role.getCode())) {
            throw new BusinessException("不能删除超级管理员角色");
        }
        long userCount = userRoleMapper.selectCount(Wrappers.<SysUserRole>lambdaQuery()
                .eq(SysUserRole::getRoleId, id));
        if (userCount > 0) {
            throw new BusinessException("该角色仍绑定用户，请先解除绑定");
        }
        rolePermissionMapper.deleteByRoleId(id);
        roleMapper.deleteById(id);
    }

    public List<Long> getPermissionIds(Long roleId) {
        requireRole(roleId);
        return rolePermissionMapper.selectPermissionIdsByRoleId(roleId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(Long roleId, AssignPermissionsRequest request) {
        requireRole(roleId);
        List<Long> permissionIds = request.permissionIds();
        List<Long> distinct = permissionIds == null
                ? List.of()
                : permissionIds.stream().filter(Objects::nonNull).distinct().toList();
        if (!distinct.isEmpty()) {
            List<SysPermission> permissions = permissionMapper.selectBatchIds(distinct);
            if (permissions.size() != distinct.size()) {
                throw new BusinessException("存在无效的权限");
            }
        }
        rolePermissionMapper.deleteByRoleId(roleId);
        LocalDateTime now = LocalDateTime.now();
        for (Long permissionId : distinct) {
            SysRolePermission relation = new SysRolePermission();
            relation.setRoleId(roleId);
            relation.setPermissionId(permissionId);
            relation.setCreatedAt(now);
            rolePermissionMapper.insert(relation);
        }
        kickoutUsers(roleId);
    }

    private void kickoutUsers(Long roleId) {
        for (Long userId : userRoleMapper.selectUserIdsByRoleId(roleId)) {
            try {
                StpUtil.kickout(Constants.ADMIN_LOGIN_ID_PREFIX + userId);
            } catch (Exception ex) {
                log.debug("踢出用户 {} 失败: {}", userId, ex.getMessage());
            }
        }
    }

    private SysRole requireRole(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "角色不存在");
        }
        return role;
    }

    private RoleVO toVO(SysRole role) {
        return new RoleVO(
                role.getId(),
                role.getCode(),
                role.getName(),
                role.getDescription(),
                role.getStatus(),
                role.getSort(),
                role.getCreatedAt() == null ? null : DATE_TIME_FORMATTER.format(role.getCreatedAt()));
    }
}
