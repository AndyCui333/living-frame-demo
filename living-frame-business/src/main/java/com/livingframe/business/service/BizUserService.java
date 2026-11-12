package com.livingframe.business.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.livingframe.business.dto.BizUserCreateRequest;
import com.livingframe.business.dto.BizUserQuery;
import com.livingframe.business.dto.BizUserUpdateRequest;
import com.livingframe.business.entity.BizUser;
import com.livingframe.business.mapper.BizUserMapper;
import com.livingframe.business.vo.BizUserVO;
import com.livingframe.common.api.PageResult;
import com.livingframe.common.api.ResultCode;
import com.livingframe.common.constant.Constants;
import com.livingframe.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 业务用户管理服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BizUserService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final BizUserMapper bizUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final RedissonClient redissonClient;

    public PageResult<BizUserVO> page(BizUserQuery query) {
        query.normalize();
        LambdaQueryWrapper<BizUser> wrapper = Wrappers.lambdaQuery();
        if (StrUtil.isNotBlank(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(BizUser::getUsername, keyword).or().like(BizUser::getNickname, keyword));
        }
        if (query.getStatus() != null) {
            wrapper.eq(BizUser::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(BizUser::getId);
        Page<BizUser> page = bizUserMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        List<BizUserVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    public BizUserVO get(Long id) {
        return toVO(requireUser(id));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(BizUserCreateRequest request) {
        String username = request.username().trim();
        RLock lock = redissonClient.getLock("living-frame:lock:biz-user:" + username);
        boolean locked = false;
        try {
            locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!locked) {
                throw new BusinessException("操作过于频繁，请稍后重试");
            }
            long exists = bizUserMapper.selectCount(Wrappers.<BizUser>lambdaQuery()
                    .eq(BizUser::getUsername, username));
            if (exists > 0) {
                throw new BusinessException(ResultCode.CONFLICT, "用户名已存在");
            }
            BizUser user = new BizUser()
                    .setUsername(username)
                    .setPassword(passwordEncoder.encode(request.password()))
                    .setNickname(StrUtil.blankToDefault(request.nickname(), username))
                    .setPhone(request.phone())
                    .setEmail(request.email())
                    .setAvatar(request.avatar())
                    .setStatus(Constants.STATUS_ENABLED);
            bizUserMapper.insert(user);
            return user.getId();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException("创建业务用户被中断，请重试");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, BizUserUpdateRequest request) {
        BizUser user = requireUser(id);
        user.setNickname(request.nickname());
        user.setPhone(request.phone());
        user.setEmail(request.email());
        user.setAvatar(request.avatar());
        bizUserMapper.updateById(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        BizUser user = requireUser(id);
        bizUserMapper.deleteById(user.getId());
        kickout(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        BizUser user = requireUser(id);
        user.setStatus(status);
        bizUserMapper.updateById(user);
        if (Objects.equals(status, Constants.STATUS_DISABLED)) {
            kickout(id);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id, String password) {
        BizUser user = requireUser(id);
        user.setPassword(passwordEncoder.encode(password));
        bizUserMapper.updateById(user);
        kickout(id);
    }

    private void kickout(Long userId) {
        try {
            cn.dev33.satoken.stp.StpUtil.kickout(Constants.BUSINESS_LOGIN_ID_PREFIX + userId);
        } catch (Exception ex) {
            log.debug("踢出业务用户 {} 失败: {}", userId, ex.getMessage());
        }
    }

    private BizUser requireUser(Long id) {
        BizUser user = bizUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "业务用户不存在");
        }
        return user;
    }

    private BizUserVO toVO(BizUser user) {
        return new BizUserVO(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getPhone(),
                user.getEmail(),
                user.getAvatar(),
                user.getStatus(),
                user.getLastLoginAt() == null ? null : DATE_TIME_FORMATTER.format(user.getLastLoginAt()),
                user.getCreatedAt() == null ? null : DATE_TIME_FORMATTER.format(user.getCreatedAt()));
    }
}
