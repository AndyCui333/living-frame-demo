package com.livingframe.ai.tool;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.livingframe.business.entity.BizUser;
import com.livingframe.business.entity.CsTicket;
import com.livingframe.business.mapper.BizUserMapper;
import com.livingframe.business.mapper.CsTicketMapper;
import com.livingframe.system.entity.SysBlacklist;
import com.livingframe.system.entity.SysRole;
import com.livingframe.system.mapper.SysBlacklistMapper;
import com.livingframe.system.mapper.SysRoleMapper;
import com.livingframe.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * 提供给 AIChat / Agent 的只读统计工具。
 */
@Component
@RequiredArgsConstructor
public class SystemTools {

    private static final int ONLINE_WINDOW_MINUTES = 5;

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final BizUserMapper bizUserMapper;
    private final SysBlacklistMapper blacklistMapper;
    private final CsTicketMapper ticketMapper;

    @Tool(description = "统计后台管理员账号数量")
    public String countAdminUsers() {
        return "后台管理员账号数量为 " + userMapper.selectCount(null);
    }

    @Tool(description = "列出系统中的角色编码")
    public String listRoleCodes() {
        String codes = roleMapper.selectList(null).stream()
                .map(SysRole::getCode)
                .limit(50)
                .collect(Collectors.joining(", "));
        return "系统角色编码：" + codes;
    }

    @Tool(description = "统计业务用户数量")
    public String countBusinessUsers() {
        return "业务用户数量为 " + bizUserMapper.selectCount(null);
    }

    @Tool(description = "统计门户当前在线用户数量（最近5分钟有心跳）")
    public String countOnlinePortalUsers() {
        Long count = bizUserMapper.selectCount(Wrappers.<BizUser>lambdaQuery()
                .eq(BizUser::getStatus, 1)
                .ge(BizUser::getLastActiveAt, LocalDateTime.now().minusMinutes(ONLINE_WINDOW_MINUTES)));
        return "门户当前在线用户数量为 " + (count == null ? 0 : count);
    }

    @Tool(description = "统计当前生效的黑名单记录数量")
    public String countActiveBlacklist() {
        Long count = blacklistMapper.selectCount(Wrappers.<SysBlacklist>lambdaQuery()
                .eq(SysBlacklist::getStatus, 1));
        return "当前生效的黑名单记录数量为 " + (count == null ? 0 : count);
    }

    @Tool(description = "统计当前待客服处理的工单数量（待派遣、已接单、处理中、待用户确认）")
    public String countPendingTickets() {
        Long count = ticketMapper.selectCount(Wrappers.<CsTicket>lambdaQuery()
                .in(CsTicket::getStatus, "PENDING", "ASSIGNED", "PROCESSING", "WAIT_CONFIRM"));
        return "当前待处理工单数量为 " + (count == null ? 0 : count);
    }

    @Tool(description = "统计已经归档完成的工单数量")
    public String countArchivedTickets() {
        Long count = ticketMapper.selectCount(Wrappers.<CsTicket>lambdaQuery()
                .eq(CsTicket::getStatus, "ARCHIVED"));
        return "已经归档完成的工单数量为 " + (count == null ? 0 : count);
    }
}
