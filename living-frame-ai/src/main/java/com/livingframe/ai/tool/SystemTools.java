package com.livingframe.ai.tool;

import com.livingframe.business.mapper.BizUserMapper;
import com.livingframe.system.entity.SysRole;
import com.livingframe.system.mapper.SysRoleMapper;
import com.livingframe.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * 提供给 Agent 的只读统计工具。
 */
@Component
@RequiredArgsConstructor
public class SystemTools {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final BizUserMapper bizUserMapper;

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
}
