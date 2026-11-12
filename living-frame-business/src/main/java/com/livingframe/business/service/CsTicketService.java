package com.livingframe.business.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.livingframe.business.dto.CsMessageView;
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
import com.livingframe.system.entity.SysUser;
import com.livingframe.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CsTicketService {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_ASSIGNED = "ASSIGNED";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_ARCHIVED = "ARCHIVED";
    public static final int MAX_DRAFTS = 5;

    private final CsTicketMapper ticketMapper;
    private final CsTicketMessageMapper messageMapper;
    private final BizUserMapper bizUserMapper;
    private final SysUserMapper sysUserMapper;
    private final CsAgentService agentService;

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
        return detailForPortal(portalUserId, ticketId);
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
            throw new BusinessException("当前工单状态不能发送消息");
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
    public CsMessageView agentReply(Long agentUserId, Long ticketId, String content) {
        CsTicket ticket = requireAgentTicket(agentUserId, ticketId);
        if (STATUS_ARCHIVED.equals(ticket.getStatus()) || STATUS_PENDING.equals(ticket.getStatus())) {
            throw new BusinessException("请先接单后再回复");
        }
        CsTicketMessage message = insertMessage(ticketId, "AGENT", agentUserId, content.trim());
        ticket.setLastMessageAt(LocalDateTime.now());
        if (STATUS_ASSIGNED.equals(ticket.getStatus())) {
            ticket.setStatus(STATUS_PROCESSING);
        }
        ticketMapper.updateById(ticket);
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
            throw new BusinessException("当前没有同类型的空闲客服");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void transfer(Long operatorUserId, Long ticketId, Long targetAgentId) {
        CsTicket ticket = requireOperatorTicket(operatorUserId, ticketId);
        if (STATUS_ARCHIVED.equals(ticket.getStatus())) {
            throw new BusinessException("已归档工单不能转接");
        }
        CsAgentProfile target = agentService.findByUserId(targetAgentId);
        if (target == null) {
            throw new BusinessException("目标客服不存在");
        }
        if (!ticket.getTicketType().equals(target.getServiceType())) {
            throw new BusinessException("只能转接给相同客服类型的坐席");
        }
        if (!"IDLE".equals(target.getOnlineStatus())) {
            throw new BusinessException("目标客服当前不是空闲状态");
        }
        if (targetAgentId.equals(ticket.getAssignedAgentId())) {
            throw new BusinessException("该工单已经由目标客服处理");
        }
        if (ticket.getAssignedAgentId() != null) {
            CsAgentProfile old = agentService.findByUserId(ticket.getAssignedAgentId());
            if (old != null) {
                agentService.markIdle(old);
            }
        }
        agentService.markBusy(target, ticketId);
        ticket.setAssignedAgentId(targetAgentId)
                .setAssignedAt(LocalDateTime.now())
                .setStatus(STATUS_ASSIGNED);
        ticketMapper.updateById(ticket);
        insertMessage(ticketId, "SYSTEM", operatorUserId,
                "工单已转接给 " + agentService.agentName(targetAgentId));
    }

    @Transactional(rollbackFor = Exception.class)
    public void archive(Long operatorUserId, Long ticketId) {
        CsTicket ticket = requireOperatorTicket(operatorUserId, ticketId);
        if (STATUS_ARCHIVED.equals(ticket.getStatus())) {
            throw new BusinessException("工单已归档");
        }
        if (STATUS_PENDING.equals(ticket.getStatus()) || STATUS_DRAFT.equals(ticket.getStatus())) {
            throw new BusinessException("当前工单还不能归档");
        }
        if (ticket.getAssignedAgentId() != null) {
            CsAgentProfile profile = agentService.findByUserId(ticket.getAssignedAgentId());
            if (profile != null) {
                agentService.markIdle(profile);
            }
        }
        ticket.setStatus(STATUS_ARCHIVED).setClosedAt(LocalDateTime.now());
        ticketMapper.updateById(ticket);
        insertMessage(ticketId, "SYSTEM", operatorUserId, "工单已截单归档，等待门户用户评价");
    }

    private void assignRandom(CsTicket ticket) {
        CsAgentProfile agent = agentService.findRandomIdle(ticket.getTicketType());
        if (agent == null) {
            ticket.setStatus(STATUS_PENDING);
            ticketMapper.updateById(ticket);
            return;
        }
        agentService.markBusy(agent, ticket.getId());
        ticket.setAssignedAgentId(agent.getUserId())
                .setAssignedAt(LocalDateTime.now())
                .setStatus(STATUS_ASSIGNED);
        ticketMapper.updateById(ticket);
        insertMessage(ticket.getId(), "SYSTEM", null,
                "工单已自动派遣给 " + agentService.agentName(agent.getUserId()));
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
}
