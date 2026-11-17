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
import com.livingframe.business.vo.BizUserActivityVO;
import com.livingframe.business.vo.BizUserStatisticsVO;
import com.livingframe.business.vo.BizUserVO;
import com.livingframe.common.api.PageResult;
import com.livingframe.common.api.ResultCode;
import com.livingframe.common.constant.Constants;
import com.livingframe.common.exception.BusinessException;
import com.livingframe.common.util.MaskUtils;
import com.livingframe.system.dto.BlacklistCreateRequest;
import com.livingframe.system.service.SystemBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 业务用户管理服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BizUserService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int ONLINE_MINUTES = 5;

    private final BizUserMapper bizUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final RedissonClient redissonClient;
    private final SystemBlacklistService blacklistService;

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

    public BizUserActivityVO activity() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime onlineAfter = now.minusMinutes(ONLINE_MINUTES);
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime sevenDaysAgo = now.minusDays(7);
        long total = count(null);
        long online = count(Wrappers.<BizUser>lambdaQuery()
                .eq(BizUser::getStatus, Constants.STATUS_ENABLED)
                .ge(BizUser::getLastActiveAt, onlineAfter));
        long todayActive = count(Wrappers.<BizUser>lambdaQuery()
                .ge(BizUser::getLastActiveAt, todayStart));
        long sevenDayActive = count(Wrappers.<BizUser>lambdaQuery()
                .ge(BizUser::getLastActiveAt, sevenDaysAgo));
        long newUsersToday = count(Wrappers.<BizUser>lambdaQuery()
                .ge(BizUser::getCreatedAt, todayStart));
        long disabled = count(Wrappers.<BizUser>lambdaQuery()
                .eq(BizUser::getStatus, Constants.STATUS_DISABLED));
        long blacklisted = blacklistService.countActiveByType("PORTAL", "USERNAME");
        return new BizUserActivityVO(
                total,
                online,
                todayActive,
                sevenDayActive,
                newUsersToday,
                disabled,
                blacklisted,
                percentage(online, total),
                percentage(todayActive, total));
    }

    public BizUserStatisticsVO statistics() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();
        LocalDateTime trendStart = today.minusDays(6).atStartOfDay();
        long total = count(null);
        long enabled = count(Wrappers.<BizUser>lambdaQuery()
                .eq(BizUser::getStatus, Constants.STATUS_ENABLED));
        long disabled = count(Wrappers.<BizUser>lambdaQuery()
                .eq(BizUser::getStatus, Constants.STATUS_DISABLED));
        long online = count(Wrappers.<BizUser>lambdaQuery()
                .eq(BizUser::getStatus, Constants.STATUS_ENABLED)
                .ge(BizUser::getLastActiveAt, now.minusMinutes(ONLINE_MINUTES)));
        long todayNew = count(Wrappers.<BizUser>lambdaQuery()
                .ge(BizUser::getCreatedAt, today.atStartOfDay()));
        long sevenDayNew = count(Wrappers.<BizUser>lambdaQuery()
                .ge(BizUser::getCreatedAt, trendStart));
        long blacklisted = blacklistService.countActiveByType("PORTAL", "USERNAME");

        List<BizUser> recentUsers = bizUserMapper.selectList(Wrappers.<BizUser>lambdaQuery()
                .ge(BizUser::getCreatedAt, trendStart));
        Map<LocalDate, Long> counts = recentUsers.stream()
                .filter(user -> user.getCreatedAt() != null)
                .collect(Collectors.groupingBy(user -> user.getCreatedAt().toLocalDate(), Collectors.counting()));
        List<BizUserStatisticsVO.DailyNew> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            trend.add(new BizUserStatisticsVO.DailyNew(
                    date.format(DateTimeFormatter.ofPattern("MM-dd")),
                    counts.getOrDefault(date, 0L)));
        }
        Map<String, Long> statusCounts = new LinkedHashMap<>();
        statusCounts.put("启用", enabled);
        statusCounts.put("禁用", disabled);
        List<BizUserStatisticsVO.StatusSlice> statusDistribution = statusCounts.entrySet().stream()
                .map(entry -> new BizUserStatisticsVO.StatusSlice(entry.getKey(), entry.getValue()))
                .toList();
        return new BizUserStatisticsVO(
                total,
                enabled,
                disabled,
                online,
                todayNew,
                sevenDayNew,
                blacklisted,
                trend,
                statusDistribution);
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
        if (!MaskUtils.isMasked(request.phone())) {
            user.setPhone(request.phone());
        }
        if (!MaskUtils.isMasked(request.email())) {
            user.setEmail(request.email());
        }
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

    @Transactional(rollbackFor = Exception.class)
    public void blacklist(Long id) {
        BizUser user = requireUser(id);
        if (!blacklistService.isBlocked("PORTAL", "USERNAME", user.getUsername())) {
            blacklistService.create(new BlacklistCreateRequest(
                    "USERNAME",
                    user.getUsername(),
                    "PORTAL",
                    "门户用户管理手动加入永久黑名单",
                    1));
        }
        user.setStatus(Constants.STATUS_DISABLED);
        bizUserMapper.updateById(user);
        kickout(id);
    }

    public void heartbeat(Long id) {
        BizUser user = bizUserMapper.selectById(id);
        if (user == null) {
            return;
        }
        user.setLastActiveAt(LocalDateTime.now());
        bizUserMapper.updateById(user);
    }

    private long count(LambdaQueryWrapper<BizUser> wrapper) {
        Long value = bizUserMapper.selectCount(wrapper);
        return value == null ? 0 : value;
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
        LocalDateTime onlineAfter = LocalDateTime.now().minusMinutes(ONLINE_MINUTES);
        boolean online = Objects.equals(user.getStatus(), Constants.STATUS_ENABLED)
                && user.getLastActiveAt() != null
                && user.getLastActiveAt().isAfter(onlineAfter);
        return new BizUserVO(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                MaskUtils.maskPhone(user.getPhone()),
                MaskUtils.maskEmail(user.getEmail()),
                user.getAvatar(),
                user.getStatus(),
                format(user.getLastLoginAt()),
                format(user.getLastActiveAt()),
                online,
                blacklistService.isBlocked("PORTAL", "USERNAME", user.getUsername()),
                format(user.getCreatedAt()));
    }

    private double percentage(long value, long total) {
        if (total <= 0) {
            return 0;
        }
        return Math.round(value * 1000.0 / total) / 10.0;
    }

    private String format(LocalDateTime value) {
        return value == null ? null : DATE_TIME_FORMATTER.format(value);
    }
}
