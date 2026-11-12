package com.livingframe.business.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.livingframe.business.dto.CsAgentView;
import com.livingframe.business.entity.CsAgentProfile;
import com.livingframe.business.mapper.CsAgentProfileMapper;
import com.livingframe.common.constant.Constants;
import com.livingframe.system.entity.SysRole;
import com.livingframe.system.entity.SysUser;
import com.livingframe.system.entity.SysUserRole;
import com.livingframe.system.mapper.SysRoleMapper;
import com.livingframe.system.mapper.SysUserMapper;
import com.livingframe.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class CsAgentService {

    private final CsAgentProfileMapper agentMapper;
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;

    @Transactional(rollbackFor = Exception.class)
    public void ensureProfiles() {
        List<SysUserRole> relations = userRoleMapper.selectList(null);
        for (SysUserRole relation : relations) {
            SysRole role = roleMapper.selectById(relation.getRoleId());
            if (role == null) {
                continue;
            }
            String serviceType = switch (role.getCode()) {
                case Constants.ROLE_CS_CHAT -> "CHAT";
                case Constants.ROLE_CS_TECH -> "TECH";
                default -> null;
            };
            if (serviceType == null) {
                continue;
            }
            Long count = agentMapper.selectCount(Wrappers.<CsAgentProfile>lambdaQuery()
                    .eq(CsAgentProfile::getUserId, relation.getUserId()));
            if (count == 0) {
                agentMapper.insert(new CsAgentProfile()
                        .setUserId(relation.getUserId())
                        .setServiceType(serviceType)
                        .setOnlineStatus("IDLE"));
                log.info("初始化客服坐席: userId={}, type={}", relation.getUserId(), serviceType);
            }
        }
    }

    public List<CsAgentView> listAgents(String serviceType) {
        List<CsAgentView> views = new ArrayList<>();
        for (CsAgentProfile profile : agentMapper.listActiveAgents()) {
            if (serviceType != null && !serviceType.equals(profile.getServiceType())) {
                continue;
            }
            SysUser user = userMapper.selectById(profile.getUserId());
            views.add(new CsAgentView(
                    profile.getId(),
                    profile.getUserId(),
                    user == null ? "未知客服" : displayName(user),
                    profile.getServiceType(),
                    profile.getOnlineStatus(),
                    profile.getCurrentTicketId()));
        }
        return views;
    }

    public CsAgentProfile findRandomIdle(String serviceType) {
        List<CsAgentProfile> candidates = agentMapper.selectList(Wrappers.<CsAgentProfile>lambdaQuery()
                .eq(CsAgentProfile::getServiceType, serviceType)
                .eq(CsAgentProfile::getOnlineStatus, "IDLE")
                .isNull(CsAgentProfile::getCurrentTicketId));
        if (candidates.isEmpty()) {
            return null;
        }
        Collections.shuffle(candidates, ThreadLocalRandom.current());
        return candidates.get(0);
    }

    public CsAgentProfile findByUserId(Long userId) {
        return agentMapper.selectOne(Wrappers.<CsAgentProfile>lambdaQuery()
                .eq(CsAgentProfile::getUserId, userId));
    }

    public void markBusy(CsAgentProfile profile, Long ticketId) {
        profile.setOnlineStatus("BUSY");
        profile.setCurrentTicketId(ticketId);
        agentMapper.updateById(profile);
    }

    public void markIdle(CsAgentProfile profile) {
        profile.setOnlineStatus("IDLE");
        profile.setCurrentTicketId(null);
        agentMapper.updateById(profile);
    }

    public String agentName(Long userId) {
        if (userId == null) {
            return "";
        }
        SysUser user = userMapper.selectById(userId);
        return user == null ? "客服" + userId : displayName(user);
    }

    private String displayName(SysUser user) {
        return user.getNickname() == null || user.getNickname().isBlank()
                ? user.getUsername()
                : user.getNickname();
    }
}
