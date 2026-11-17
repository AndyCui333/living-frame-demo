package com.livingframe.business.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.livingframe.business.dto.CsAgentView;
import com.livingframe.business.entity.CsAgentProfile;
import com.livingframe.business.entity.CsTicket;
import com.livingframe.business.mapper.CsAgentProfileMapper;
import com.livingframe.business.mapper.CsTicketMapper;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class CsAgentService {

    public static final int MAX_CONCURRENT = 5;
    private static final long ONLINE_TIMEOUT_SECONDS = 120;

    private final CsAgentProfileMapper agentMapper;
    private final CsTicketMapper ticketMapper;
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
                        .setOnlineStatus("OFFLINE"));
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
            int activeCount = activeCount(profile.getUserId());
            views.add(new CsAgentView(
                    profile.getId(),
                    profile.getUserId(),
                    user == null ? "未知客服" : displayName(user),
                    profile.getServiceType(),
                    computedStatus(profile, activeCount),
                    profile.getCurrentTicketId(),
                    activeCount,
                    MAX_CONCURRENT));
        }
        return views;
    }

    /**
     * 选择在线且未满 5 个会话的坐席。
     */
    public CsAgentProfile findAvailableOnline(String serviceType) {
        List<CsAgentProfile> candidates = new ArrayList<>();
        for (CsAgentProfile profile : agentMapper.listActiveAgents()) {
            if (!serviceType.equals(profile.getServiceType())) {
                continue;
            }
            int count = activeCount(profile.getUserId());
            if ("IDLE".equals(computedStatus(profile, count)) && count < MAX_CONCURRENT) {
                candidates.add(profile);
            }
        }
        return random(candidates);
    }

    public CsAgentProfile findRandomByType(String serviceType) {
        List<CsAgentProfile> candidates = agentMapper.listActiveAgents().stream()
                .filter(profile -> serviceType.equals(profile.getServiceType()))
                .filter(profile -> activeCount(profile.getUserId()) < MAX_CONCURRENT)
                .toList();
        return random(new ArrayList<>(candidates));
    }

    public boolean isOnlineAgent(CsAgentProfile profile) {
        return profile != null && isOnline(profile);
    }

    public boolean hasOnlineAgents(String serviceType) {
        return agentMapper.listActiveAgents().stream()
                .filter(profile -> serviceType.equals(profile.getServiceType()))
                .anyMatch(this::isOnline);
    }

    public CsAgentProfile findByUserId(Long userId) {
        return agentMapper.selectOne(Wrappers.<CsAgentProfile>lambdaQuery()
                .eq(CsAgentProfile::getUserId, userId));
    }

    public void heartbeat(Long userId) {
        CsAgentProfile profile = findByUserId(userId);
        if (profile == null) {
            return;
        }
        profile.setOnlineStatus("IDLE");
        profile.setLastSeenAt(LocalDateTime.now());
        agentMapper.updateById(profile);
    }

    public void offline(Long userId) {
        CsAgentProfile profile = findByUserId(userId);
        if (profile == null) {
            return;
        }
        profile.setOnlineStatus("OFFLINE");
        agentMapper.updateById(profile);
    }

    public void setCurrentTicket(CsAgentProfile profile, Long ticketId) {
        if (profile == null) {
            return;
        }
        profile.setCurrentTicketId(ticketId);
        profile.setLastSeenAt(LocalDateTime.now());
        agentMapper.updateById(profile);
    }

    public void clearCurrentTicket(CsAgentProfile profile, Long ticketId) {
        if (profile == null || ticketId == null) {
            return;
        }
        if (ticketId.equals(profile.getCurrentTicketId())) {
            profile.setCurrentTicketId(null);
        }
        if (isOnline(profile) && activeCount(profile.getUserId()) == 0) {
            profile.setOnlineStatus("IDLE");
        }
        agentMapper.updateById(profile);
    }

    public int activeCount(Long userId) {
        Long count = ticketMapper.selectCount(Wrappers.<CsTicket>lambdaQuery()
                .eq(CsTicket::getAssignedAgentId, userId)
                .in(CsTicket::getStatus, List.of("ASSIGNED", "PROCESSING", "WAIT_CONFIRM")));
        return count == null ? 0 : count.intValue();
    }

    public String agentName(Long userId) {
        if (userId == null) {
            return "";
        }
        SysUser user = userMapper.selectById(userId);
        return user == null ? "客服" + userId : displayName(user);
    }

    private CsAgentProfile random(List<CsAgentProfile> candidates) {
        if (candidates.isEmpty()) {
            return null;
        }
        Collections.shuffle(candidates, ThreadLocalRandom.current());
        return candidates.get(0);
    }

    private boolean isOnline(CsAgentProfile profile) {
        return !"OFFLINE".equals(profile.getOnlineStatus())
                && profile.getLastSeenAt() != null
                && profile.getLastSeenAt().isAfter(LocalDateTime.now().minusSeconds(ONLINE_TIMEOUT_SECONDS));
    }

    private String computedStatus(CsAgentProfile profile, int activeCount) {
        if (!isOnline(profile)) {
            return "OFFLINE";
        }
        return activeCount >= MAX_CONCURRENT ? "BUSY" : "IDLE";
    }

    private String displayName(SysUser user) {
        return user.getNickname() == null || user.getNickname().isBlank()
                ? user.getUsername()
                : user.getNickname();
    }
}
