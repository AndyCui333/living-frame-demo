package com.livingframe.system.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.livingframe.common.api.PageResult;
import com.livingframe.common.api.ResultCode;
import com.livingframe.common.context.LoginUserContext;
import com.livingframe.common.exception.BusinessException;
import com.livingframe.common.model.LoginUser;
import com.livingframe.system.dto.BlacklistCreateRequest;
import com.livingframe.system.dto.BlacklistQuery;
import com.livingframe.system.entity.SysBlacklist;
import com.livingframe.system.mapper.SysBlacklistMapper;
import com.livingframe.system.vo.BlacklistVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * 系统黑名单：所有记录均为永久生效，统一存储在 sys_blacklist。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemBlacklistService {

    private final SysBlacklistMapper blacklistMapper;

    public boolean isBlocked(String targetType, String targetValue) {
        if (StrUtil.isBlank(targetType) || StrUtil.isBlank(targetValue)) {
            return false;
        }
        Long count = blacklistMapper.selectCount(Wrappers.<SysBlacklist>lambdaQuery()
                .eq(SysBlacklist::getTargetType, normalizeType(targetType))
                .eq(SysBlacklist::getTargetValue, targetValue.trim())
                .eq(SysBlacklist::getStatus, 1));
        return count != null && count > 0;
    }

    /**
     * 登录失败达到阈值后自动把来源 IP 加入永久系统黑名单。
     */
    public void autoBlockIp(String ip, String reason) {
        if (StrUtil.isBlank(ip)) {
            return;
        }
        try {
            SysBlacklist existing = blacklistMapper.selectOne(Wrappers.<SysBlacklist>lambdaQuery()
                    .eq(SysBlacklist::getTargetType, "IP")
                    .eq(SysBlacklist::getTargetValue, ip.trim()));
            if (existing != null) {
                blacklistMapper.update(null, Wrappers.<SysBlacklist>lambdaUpdate()
                        .eq(SysBlacklist::getId, existing.getId())
                        .set(SysBlacklist::getStatus, 1)
                        .set(SysBlacklist::getReason, reason));
                return;
            }
            SysBlacklist record = new SysBlacklist()
                    .setTargetType("IP")
                    .setTargetValue(ip.trim())
                    .setReason(reason)
                    .setStatus(1)
                    .setCreatedBy("SYSTEM");
            blacklistMapper.insert(record);
        } catch (Exception ex) {
            log.warn("自动加入系统黑名单失败: ip={}, error={}", ip, ex.getMessage());
        }
    }

    public PageResult<BlacklistVO> page(BlacklistQuery query) {
        query.normalize();
        LambdaQueryWrapper<SysBlacklist> wrapper = Wrappers.lambdaQuery();
        if (StrUtil.isNotBlank(query.getTargetType())) {
            wrapper.eq(SysBlacklist::getTargetType, normalizeType(query.getTargetType()));
        }
        if (StrUtil.isNotBlank(query.getTargetValue())) {
            wrapper.like(SysBlacklist::getTargetValue, query.getTargetValue().trim());
        }
        if (query.getStatus() != null) {
            wrapper.eq(SysBlacklist::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(SysBlacklist::getId);
        Page<SysBlacklist> page = blacklistMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        List<BlacklistVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(BlacklistCreateRequest request) {
        String type = normalizeType(request.targetType());
        String value = request.targetValue().trim();
        Long exists = blacklistMapper.selectCount(Wrappers.<SysBlacklist>lambdaQuery()
                .eq(SysBlacklist::getTargetType, type)
                .eq(SysBlacklist::getTargetValue, value));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "该目标已在系统黑名单中");
        }
        LoginUser loginUser = LoginUserContext.get();
        SysBlacklist record = new SysBlacklist()
                .setTargetType(type)
                .setTargetValue(value)
                .setReason(request.reason().trim())
                .setStatus(request.status() == null ? 1 : request.status())
                .setCreatedBy(loginUser == null ? "SYSTEM" : loginUser.getUsername());
        blacklistMapper.insert(record);
        return record.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        SysBlacklist entity = require(id);
        entity.setStatus(status);
        blacklistMapper.updateById(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        require(id);
        blacklistMapper.deleteById(id);
    }

    private SysBlacklist require(Long id) {
        SysBlacklist entity = blacklistMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "系统黑名单记录不存在");
        }
        return entity;
    }

    private String normalizeType(String type) {
        String normalized = type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
        if (!"IP".equals(normalized) && !"USERNAME".equals(normalized)) {
            throw new BusinessException("黑名单类型只能为 IP 或 USERNAME");
        }
        return normalized;
    }

    private BlacklistVO toVO(SysBlacklist entity) {
        return new BlacklistVO(
                entity.getId(),
                entity.getTargetType(),
                entity.getTargetValue(),
                entity.getReason(),
                entity.getStatus(),
                entity.getCreatedBy());
    }
}
