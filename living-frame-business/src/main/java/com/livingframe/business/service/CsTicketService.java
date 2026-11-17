package com.livingframe.business.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.livingframe.business.dto.CsAgentView;
import com.livingframe.business.dto.CsMessageView;
import com.livingframe.business.dto.CsOnlineInbox;
import com.livingframe.business.dto.CsRankingView;
import com.livingframe.business.dto.CsTicketDetail;
import com.livingframe.business.dto.CsTicketSaveRequest;
import com.livingframe.business.dto.CsTicketView;
import com.livingframe.business.entity.BizUser;
import com.livingframe.business.entity.CsAgentProfile;
import com.livingframe.business.entity.CsTicket;
import com.livingframe.business.entity.CsTicketMessage;
import com.livingframe.business.mapper.BizUserMapper;
import com.livingframe.business.mapper.CsTicketMapper;
import com.livingframe.business.mapper.CsTicketMessageMapper;
import com.livingframe.common.context.LoginUserContext;
import com.livingframe.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CsTicketService {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_ASSIGNED = "ASSIGNED";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_WAIT_CONFIRM = "WAIT_CONFIRM";
    public static final String STATUS_ARCHIVED = "ARCHIVED";
    public static final int MAX_DRAFTS = 5;

    private static final String WORKING_HOURS_TIP =
            "当前可能不在客服工作时间，已为你登记工单，客服上线后会尽快回复你。";
    private static final String BUSY_TIP =
            "当前在线客服繁忙，已为你排队，请耐心等待。";

    private final CsTicketMapper ticketMapper;
    private final CsTicketMessageMapper messageMapper;
    private final BizUserMapper bizUserMapper;
    private final CsAgentService agentService;
    private final BizNotificationService notificationService;
    private final ChatPresenceService chatPresenceService;

    @Transactional(rollbackFor = Exception.class)
    public Long createDraft(Long portalUserId, CsTicketSaveRequest request) {
        long drafts = ticketMapper.selectCount(Wrappers.<CsTicket>lambdaQuery()
                .eq(CsTicket::getPortalUserId, portalUserId)
                .eq(CsTicket::getStatus, STATUS_DRAFT));
        if (drafts >= MAX_DRAFTS) {
            throw new BusinessException("草稿箱最多保存 " + MAX_DRAFTS + " 条，请先提交或删除草稿");
        }
        CsTicket ticket = new CsTicket()
                .setTicketNo(nextTicketNo())
                .setPortalUserId(portalUserId)
                .setTitle(request.title().trim())
                .setContent(request.content().trim())
                .setTicketType(request.ticketType())
                .setStatus(STATUS_DRAFT)
                .setLastMessageAt(LocalDateTime.now());
        ticketMapper.insert(ticket);
        return ticket.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateDraft(Long portalUserId, Long ticketId, CsTicketSaveRequest request) {
        CsTicket ticket = requirePortalTicket(portalUserId, ticketId);
        if (!STATUS_DRAFT.equals(ticket.getStatus())) {
            throw new BusinessException("只有草稿可以修改");
        }
        ticket.setTitle(request.title().trim())
                .setContent(request.content().trim())
                .setTicketType(request.ticketType());
        ticketMapper.updateById(ticket);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteDraft(Long portalUserId, Long ticketId) {
        CsTicket ticket = requirePortalTicket(portalUserId, ticketId);
        if (!STATUS_DRAFT.equals(ticket.getStatus())) {
            throw new BusinessException("只有草稿可以删除");
        }
        ticketMapper.deleteById(ticketId);
    }

    @Transactional(rollbackFor = Exception.class)
    public CsTicketDetail submit(Long portalUserId, Long ticketId) {
        CsTicket ticket = requirePortalTicket(portalUserId, ticketId);
        if (!STATUS_DRAFT.equals(ticket.getStatus())) {
            throw new BusinessException("该工单已提交");
        }
        if (StrUtil.isBlank(ticket.getTitle()) || StrUtil.isBlank(ticket.getContent())) {
            throw new BusinessException("工单标题和描述不能为空");
        }
        ticket.setStatus(STATUS_PENDING).setLastMessageAt(LocalDateTime.now());
        ticketMapper.updateById(ticket);
        insertMessage(ticketId, "PORTAL", portalUserId, ticket.getContent());
        assignRandom(ticket);
        return detail(ticket);
    }

    /**
     * 门户聊天入口：2 分钟内且有进行中的工单时继续，否则自动创建新的聊天工单。
     * 新工单永远是 CHAT 类型；技术工单只能由聊天客服转接产生。
     */
    @Transactional(rollbackFor = Exception.class)
    public CsTicketDetail chat(Long portalUserId, Long ticketId, String content, String ticketType) {
        CsTicket existing;
        if (ticketId != null) {
            existing = ticketMapper.selectById(ticketId);
            if (existing != null && !existing.getPortalUserId().equals(portalUserId)) {
                throw new BusinessException("工单不存在或无权访问");
            }
        } else {
            // 未结束的工单优先复用，避免同一个用户产生多个并行工单
            existing = ticketMapper.selectOne(Wrappers.<CsTicket>lambdaQuery()
                    .eq(CsTicket::getPortalUserId, portalUserId)
                    .notIn(CsTicket::getStatus, List.of(STATUS_ARCHIVED, STATUS_DRAFT))
                    .orderByDesc(CsTicket::getUpdatedAt)
                    .orderByDesc(CsTicket::getId)
                    .last("LIMIT 1"));
        }
        if (existing != null
                && !STATUS_ARCHIVED.equals(existing.getStatus())
                && !STATUS_DRAFT.equals(existing.getStatus())) {
            insertMessage(existing.getId(), "PORTAL", portalUserId, content.trim());
            existing.setLastMessageAt(LocalDateTime.now());
            ticketMapper.updateById(existing);
            return detail(existing);
        }
        CsTicket ticket = new CsTicket()
                .setTicketNo(nextTicketNo())
                .setPortalUserId(portalUserId)
                .setTitle(titleFrom(content))
                .setContent(content.trim())
                .setTicketType("CHAT")
                .setStatus(STATUS_PENDING)
                .setLastMessageAt(LocalDateTime.now());
        ticketMapper.insert(ticket);
        insertMessage(ticket.getId(), "PORTAL", portalUserId, content.trim());
        assignRandom(ticket);
        return detail(ticket);
    }

    public List<CsTicketView> listPortalTickets(Long portalUserId) {
        return ticketMapper.selectList(Wrappers.<CsTicket>lambdaQuery()
                        .eq(CsTicket::getPortalUserId, portalUserId)
                        .orderByDesc(CsTicket::getUpdatedAt)
                        .orderByDesc(CsTicket::getId))
                .stream().map(this::toView).toList();
    }

    public CsTicketDetail detailForPortal(Long portalUserId, Long ticketId) {
        CsTicket ticket = requirePortalTicket(portalUserId, ticketId);
        return detail(ticket);
    }

    @Transactional(rollbackFor = Exception.class)
    public CsMessageView portalMessage(Long portalUserId, Long ticketId, String content) {
        CsTicket ticket = requirePortalTicket(portalUserId, ticketId);
        if (STATUS_DRAFT.equals(ticket.getStatus()) || STATUS_ARCHIVED.equals(ticket.getStatus())) {
            throw new BusinessException("该工单已结束，请发起新工单");
        }
        CsTicketMessage message = insertMessage(ticketId, "PORTAL", portalUserId, content.trim());
        ticket.setLastMessageAt(LocalDateTime.now());
        ticketMapper.updateById(ticket);
        return toMessageView(message);
    }

    @Transactional(rollbackFor = Exception.class)
    public void feedback(Long portalUserId, Long ticketId, Integer rating, String feedback) {
        CsTicket ticket = requirePortalTicket(portalUserId, ticketId);
        if (!STATUS_ARCHIVED.equals(ticket.getStatus())) {
            throw new BusinessException("工单归档后才能评价");
        }
        if (ticket.getRating() != null) {
            throw new BusinessException("该工单已评价");
        }
        ticket.setRating(rating).setFeedback(StrUtil.trimToEmpty(feedback));
        ticketMapper.updateById(ticket);
    }

    public List<CsTicketView> listAgentTickets(Long agentUserId, String serviceType) {
        boolean superAdmin = LoginUserContext.require().isSuperAdmin();
        CsAgentProfile profile = superAdmin ? null : requireAgent(agentUserId);
        String type = superAdmin ? serviceType : profile.getServiceType();
        var wrapper = Wrappers.<CsTicket>lambdaQuery()
                .ne(CsTicket::getStatus, STATUS_DRAFT)
                .orderByDesc(CsTicket::getUpdatedAt)
                .orderByDesc(CsTicket::getId);
        if (StrUtil.isNotBlank(type)) {
            wrapper.eq(CsTicket::getTicketType, type);
        }
        if (!superAdmin) {
            wrapper.and(w -> w.eq(CsTicket::getAssignedAgentId, agentUserId)
                    .or(n -> n.eq(CsTicket::getStatus, STATUS_PENDING)
                            .eq(CsTicket::getTicketType, profile.getServiceType())));
        }
        return ticketMapper.selectList(wrapper).stream().map(this::toView).toList();
    }

    public CsTicketDetail detailForAgent(Long agentUserId, Long ticketId) {
        CsTicket ticket = requireAgentTicket(agentUserId, ticketId);
        return detail(ticket);
    }

    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long agentUserId, Long ticketId) {
        CsTicket ticket = requireAgentTicket(agentUserId, ticketId);
        ticket.setAgentReadAt(LocalDateTime.now());
        ticketMapper.updateById(ticket);
        chatPresenceService.touchAgent(agentUserId, ticketId);
    }

    public void leaveAgentTicket(Long agentUserId) {
        chatPresenceService.leaveAgent(agentUserId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void markPortalRead(Long portalUserId, Long ticketId) {
        requirePortalTicket(portalUserId, ticketId);
        chatPresenceService.touchPortal(portalUserId, ticketId);
        notificationService.markTargetRead(portalUserId, "CS_TICKET", ticketId);
    }

    public void leavePortalTicket(Long portalUserId) {
        chatPresenceService.leavePortal(portalUserId);
    }

    @Transactional(rollbackFor = Exception.class)
    public CsMessageView agentReply(Long agentUserId, Long ticketId, String content) {
        CsTicket ticket = requireAgentTicket(agentUserId, ticketId);
        if (STATUS_ARCHIVED.equals(ticket.getStatus()) || STATUS_PENDING.equals(ticket.getStatus())) {
            throw new BusinessException("请先接单后再回复");
        }
        CsTicketMessage message = insertMessage(ticketId, "AGENT", agentUserId, content.trim());
        ticket.setLastMessageAt(LocalDateTime.now());
        ticket.setAgentReadAt(LocalDateTime.now());
        if (STATUS_ASSIGNED.equals(ticket.getStatus())) {
            ticket.setStatus(STATUS_PROCESSING);
        }
        ticketMapper.updateById(ticket);
        if (!chatPresenceService.isPortalViewing(ticket.getPortalUserId(), ticketId)) {
            notificationService.create(
                    ticket.getPortalUserId(),
                    "CS_MESSAGE",
                    "客服回复",
                    agentService.agentName(agentUserId) + "：" + content.trim(),
                    "CS_TICKET",
                    ticketId);
        }
        return toMessageView(message);
    }

    @Transactional(rollbackFor = Exception.class)
    public void dispatch(Long ticketId) {
        CsTicket ticket = requireExisting(ticketId);
        if (!STATUS_PENDING.equals(ticket.getStatus())) {
            throw new BusinessException("只有待派遣工单可以派遣");
        }
        assignRandom(ticket);
        if (STATUS_PENDING.equals(ticket.getStatus())) {
            throw new BusinessException("当前没有可接入的在线客服");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void transfer(Long operatorUserId, Long ticketId, Long targetAgentId) {
        CsTicket ticket = requireOperatorTicket(operatorUserId, ticketId);
        if (STATUS_ARCHIVED.equals(ticket.getStatus())) {
            throw new BusinessException("已归档工单不能转接，请发起新工单");
        }
        if (STATUS_WAIT_CONFIRM.equals(ticket.getStatus())) {
            throw new BusinessException("工单正在等待用户确认完结，不能转接");
        }
        CsAgentProfile target = agentService.findByUserId(targetAgentId);
        if (target == null) {
            throw new BusinessException("目标客服不存在");
        }
        if ("TECH".equals(ticket.getTicketType()) && !"TECH".equals(target.getServiceType())) {
            throw new BusinessException("技术工单只能转接给技术客服");
        }
        if (!agentService.isOnlineAgent(target)) {
            throw new BusinessException("目标客服当前不在线");
        }
        if (agentService.activeCount(targetAgentId) >= CsAgentService.MAX_CONCURRENT) {
            throw new BusinessException("目标客服会话已满，请选择其他客服");
        }
        if (targetAgentId.equals(ticket.getAssignedAgentId())) {
            throw new BusinessException("该工单已经由目标客服处理");
        }
        if (ticket.getAssignedAgentId() != null) {
            CsAgentProfile old = agentService.findByUserId(ticket.getAssignedAgentId());
            if (old != null) {
                agentService.clearCurrentTicket(old, ticketId);
            }
        }
        boolean escalate = "CHAT".equals(ticket.getTicketType()) && "TECH".equals(target.getServiceType());
        if (escalate) {
            ticket.setTicketType("TECH");
        }
        agentService.setCurrentTicket(target, ticketId);
        ticket.setAssignedAgentId(targetAgentId)
                .setAssignedAt(LocalDateTime.now())
                .setAgentReadAt(null)
                .setStatus(STATUS_ASSIGNED);
        ticketMapper.updateById(ticket);
        insertMessage(ticketId, "SYSTEM", operatorUserId,
                escalate ? "工单已升级并转接给技术客服 " + agentService.agentName(targetAgentId)
                        : "工单已转接给 " + agentService.agentName(targetAgentId));
    }

    @Transactional(rollbackFor = Exception.class)
    public void requestComplete(Long operatorUserId, Long ticketId) {
        CsTicket ticket = requireOperatorTicket(operatorUserId, ticketId);
        if (STATUS_ARCHIVED.equals(ticket.getStatus())) {
            throw new BusinessException("工单已完结");
        }
        if (STATUS_WAIT_CONFIRM.equals(ticket.getStatus())) {
            throw new BusinessException("已提交完结申请，等待用户确认");
        }
        if (!STATUS_ASSIGNED.equals(ticket.getStatus()) && !STATUS_PROCESSING.equals(ticket.getStatus())) {
            throw new BusinessException("当前工单还不能申请完结");
        }
        ticket.setStatus(STATUS_WAIT_CONFIRM)
                .setLastMessageAt(LocalDateTime.now());
        ticketMapper.updateById(ticket);
        insertMessage(ticketId, "SYSTEM", operatorUserId,
                "客服已申请结束本次服务，请门户用户确认。确认前工单仍保持进行中。");
    }

    @Transactional(rollbackFor = Exception.class)
    public void confirmComplete(Long portalUserId, Long ticketId) {
        CsTicket ticket = requirePortalTicket(portalUserId, ticketId);
        if (!STATUS_WAIT_CONFIRM.equals(ticket.getStatus())) {
            throw new BusinessException("当前工单没有待确认的完结申请");
        }
        if (ticket.getAssignedAgentId() != null) {
            CsAgentProfile profile = agentService.findByUserId(ticket.getAssignedAgentId());
            if (profile != null) {
                agentService.clearCurrentTicket(profile, ticketId);
            }
        }
        ticket.setStatus(STATUS_ARCHIVED).setClosedAt(LocalDateTime.now());
        ticketMapper.updateById(ticket);
        insertMessage(ticketId, "SYSTEM", portalUserId, "用户已确认工单完成，本次服务结束。");
    }

    @Transactional(rollbackFor = Exception.class)
    public void rejectComplete(Long portalUserId, Long ticketId) {
        CsTicket ticket = requirePortalTicket(portalUserId, ticketId);
        if (!STATUS_WAIT_CONFIRM.equals(ticket.getStatus())) {
            throw new BusinessException("当前工单没有待确认的完结申请");
        }
        ticket.setStatus(STATUS_PROCESSING)
                .setLastMessageAt(LocalDateTime.now());
        ticketMapper.updateById(ticket);
        insertMessage(ticketId, "SYSTEM", portalUserId, "用户暂未确认完结，工单继续处理。");
    }

    public CsOnlineInbox onlineInbox(Long agentUserId) {
        CsAgentProfile profile = requireAgent(agentUserId);
        int activeCount = agentService.activeCount(agentUserId);
        Long activeTicketId = chatPresenceService.activeAgentTicket(agentUserId);
        var query = Wrappers.<CsTicket>lambdaQuery()
                .eq(CsTicket::getAssignedAgentId, agentUserId)
                .in(CsTicket::getStatus, List.of(STATUS_ASSIGNED, STATUS_PROCESSING, STATUS_WAIT_CONFIRM))
                .and(w -> w.isNull(CsTicket::getAgentReadAt)
                        .or().apply("last_message_at > agent_read_at"))
                .orderByAsc(CsTicket::getLastMessageAt);
        if (activeTicketId != null) {
            query.ne(CsTicket::getId, activeTicketId);
        }
        List<CsTicket> unread = ticketMapper.selectList(query);
        List<CsTicketView> views = unread.stream().map(this::toView).toList();
        return new CsOnlineInbox(
                activeCount,
                CsAgentService.MAX_CONCURRENT,
                views.size(),
                views.isEmpty() ? null : views.get(0).id(),
                views);
    }

    public List<CsRankingView> ranking() {
        List<CsAgentView> agents = agentService.listAgents(null);
        List<RankRecord> records = new ArrayList<>();
        for (CsAgentView agent : agents) {
            List<CsTicket> processed = ticketMapper.selectList(Wrappers.<CsTicket>lambdaQuery()
                    .eq(CsTicket::getAssignedAgentId, agent.userId())
                    .eq(CsTicket::getStatus, STATUS_ARCHIVED));
            double average = processed.stream()
                    .filter(ticket -> ticket.getRating() != null)
                    .mapToInt(CsTicket::getRating)
                    .average()
                    .orElse(0);
            long ratingCount = processed.stream().filter(ticket -> ticket.getRating() != null).count();
            records.add(new RankRecord(agent, processed.size(), average, ratingCount));
        }
        records.sort(Comparator
                .comparingLong(RankRecord::processedCount).reversed()
                .thenComparing(Comparator.comparingDouble(RankRecord::averageRating).reversed())
                .thenComparing(record -> record.agent().name()));
        List<CsRankingView> result = new ArrayList<>();
        for (int i = 0; i < records.size(); i++) {
            RankRecord record = records.get(i);
            result.add(new CsRankingView(
                    i + 1,
                    record.agent().userId(),
                    record.agent().name(),
                    record.agent().serviceType(),
                    record.processedCount(),
                    Math.round(record.averageRating() * 10.0) / 10.0,
                    record.ratingCount()));
        }
        return result;
    }

    private void assignRandom(CsTicket ticket) {
        if ("TECH".equals(ticket.getTicketType())) {
            CsAgentProfile tech = agentService.findAvailableOnline("TECH");
            if (tech == null) {
                ticket.setStatus(STATUS_PENDING);
                ticketMapper.updateById(ticket);
                insertMessage(ticket.getId(), "SYSTEM", null, BUSY_TIP);
                return;
            }
            assignTo(ticket, tech, "工单已派遣给技术客服 " + agentService.agentName(tech.getUserId()));
            return;
        }

        CsAgentProfile online = agentService.findAvailableOnline("CHAT");
        if (online != null) {
            assignTo(ticket, online, "工单已自动派遣给 " + agentService.agentName(online.getUserId()));
            return;
        }
        if (agentService.hasOnlineAgents("CHAT")) {
            ticket.setStatus(STATUS_PENDING);
            ticketMapper.updateById(ticket);
            insertMessage(ticket.getId(), "SYSTEM", null, BUSY_TIP);
            return;
        }
        CsAgentProfile fallback = agentService.findRandomByType("CHAT");
        if (fallback != null) {
            assignTo(ticket, fallback, WORKING_HOURS_TIP);
            return;
        }
        ticket.setStatus(STATUS_PENDING);
        ticketMapper.updateById(ticket);
        insertMessage(ticket.getId(), "SYSTEM", null, WORKING_HOURS_TIP);
    }

    private void assignTo(CsTicket ticket, CsAgentProfile agent, String notice) {
        agentService.setCurrentTicket(agent, ticket.getId());
        ticket.setAssignedAgentId(agent.getUserId())
                .setAssignedAt(LocalDateTime.now())
                .setAgentReadAt(null)
                .setStatus(STATUS_ASSIGNED);
        ticketMapper.updateById(ticket);
        insertMessage(ticket.getId(), "SYSTEM", null, notice);
    }

    private CsTicketDetail detail(CsTicket ticket) {
        List<CsMessageView> messages = messageMapper.selectList(Wrappers.<CsTicketMessage>lambdaQuery()
                        .eq(CsTicketMessage::getTicketId, ticket.getId())
                        .orderByAsc(CsTicketMessage::getId))
                .stream().map(this::toMessageView).toList();
        return new CsTicketDetail(toView(ticket), messages);
    }

    private CsTicketView toView(CsTicket ticket) {
        BizUser portalUser = bizUserMapper.selectById(ticket.getPortalUserId());
        String portalName = portalUser == null ? "门户用户"
                : StrUtil.blankToDefault(portalUser.getNickname(), portalUser.getUsername());
        boolean unread = ticket.getAssignedAgentId() != null
                && (ticket.getAgentReadAt() == null
                || (ticket.getLastMessageAt() != null && ticket.getLastMessageAt().isAfter(ticket.getAgentReadAt())));
        return new CsTicketView(
                ticket.getId(),
                ticket.getTicketNo(),
                ticket.getTitle(),
                ticket.getContent(),
                ticket.getTicketType(),
                ticket.getStatus(),
                portalName,
                ticket.getAssignedAgentId(),
                agentService.agentName(ticket.getAssignedAgentId()),
                unread,
                ticket.getRating(),
                ticket.getFeedback(),
                ticket.getAssignedAt(),
                ticket.getLastMessageAt(),
                ticket.getClosedAt(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt());
    }

    private CsMessageView toMessageView(CsTicketMessage message) {
        String name;
        if ("PORTAL".equals(message.getSenderType())) {
            BizUser user = bizUserMapper.selectById(message.getSenderId());
            name = user == null ? "门户用户" : StrUtil.blankToDefault(user.getNickname(), user.getUsername());
        } else if ("AGENT".equals(message.getSenderType())) {
            name = agentService.agentName(message.getSenderId());
        } else {
            name = "系统";
        }
        return new CsMessageView(
                message.getId(), message.getSenderType(), name,
                message.getContent(), message.getCreatedAt());
    }

    private CsTicketMessage insertMessage(Long ticketId, String senderType, Long senderId, String content) {
        CsTicketMessage message = new CsTicketMessage()
                .setTicketId(ticketId)
                .setSenderType(senderType)
                .setSenderId(senderId)
                .setContent(content);
        messageMapper.insert(message);
        return message;
    }

    private CsTicket requirePortalTicket(Long portalUserId, Long ticketId) {
        CsTicket ticket = requireExisting(ticketId);
        if (!ticket.getPortalUserId().equals(portalUserId)) {
            throw new BusinessException("工单不存在或无权访问");
        }
        return ticket;
    }

    private CsAgentProfile requireAgent(Long agentUserId) {
        CsAgentProfile profile = agentService.findByUserId(agentUserId);
        if (profile == null) {
            throw new BusinessException("当前账号不是客服坐席");
        }
        return profile;
    }

    private CsTicket requireAgentTicket(Long agentUserId, Long ticketId) {
        CsTicket ticket = requireExisting(ticketId);
        boolean superAdmin = LoginUserContext.require().isSuperAdmin();
        if (superAdmin) {
            return ticket;
        }
        CsAgentProfile profile = requireAgent(agentUserId);
        boolean assigned = agentUserId.equals(ticket.getAssignedAgentId());
        boolean pendingSameType = STATUS_PENDING.equals(ticket.getStatus())
                && profile.getServiceType().equals(ticket.getTicketType());
        if (!assigned && !pendingSameType) {
            throw new BusinessException("工单不存在或无权访问");
        }
        return ticket;
    }

    private CsTicket requireOperatorTicket(Long operatorUserId, Long ticketId) {
        CsTicket ticket = requireExisting(ticketId);
        boolean superAdmin = LoginUserContext.require().isSuperAdmin();
        if (!superAdmin && !operatorUserId.equals(ticket.getAssignedAgentId())) {
            throw new BusinessException("只有接单客服可以执行该操作");
        }
        return ticket;
    }

    private CsTicket requireExisting(Long ticketId) {
        CsTicket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw new BusinessException("工单不存在");
        }
        return ticket;
    }

    private String nextTicketNo() {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        return "CS" + date + IdUtil.fastSimpleUUID().substring(0, 6).toUpperCase();
    }

    private String titleFrom(String content) {
        String title = content.replaceAll("\\s+", " ").trim();
        return title.length() > 40 ? title.substring(0, 40) + "…" : title;
    }

    private record RankRecord(CsAgentView agent, long processedCount, double averageRating, long ratingCount) {
    }
}
