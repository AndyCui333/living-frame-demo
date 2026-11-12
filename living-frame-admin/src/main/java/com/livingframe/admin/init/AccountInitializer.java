package com.livingframe.admin.init;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.livingframe.business.entity.BizUser;
import com.livingframe.business.mapper.BizUserMapper;
import com.livingframe.common.constant.Constants;
import com.livingframe.system.entity.SysRole;
import com.livingframe.system.entity.SysUser;
import com.livingframe.system.entity.SysUserRole;
import com.livingframe.system.mapper.SysRoleMapper;
import com.livingframe.system.mapper.SysUserMapper;
import com.livingframe.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 演示账号初始化：只补缺，不覆盖已有账号。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountInitializer implements ApplicationRunner {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final BizUserMapper bizUserMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.init.admin-password:Admin@123}")
    private String adminPassword;

    @Value("${app.init.sysadmin-password:Sys@123}")
    private String sysadminPassword;

    @Value("${app.init.viewer-password:View@123}")
    private String viewerPassword;

    @Value("${app.init.business-password:Biz@123}")
    private String businessPassword;

    @Value("${app.init.blacklist-password:Blacklist@123}")
    private String blacklistPassword;

    @Value("${app.init.smoke-password:Smoke@123}")
    private String smokePassword;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) {
        createAdmin("admin", adminPassword, "超级管理员", Constants.ROLE_SUPER_ADMIN);
        createAdmin("sysadmin", sysadminPassword, "系统管理员", Constants.ROLE_SYSTEM_ADMIN);
        createAdmin("viewer", viewerPassword, "只读用户", Constants.ROLE_VIEWER);
        createAdmin("blacklist_test", blacklistPassword, "黑名单测试账号", Constants.ROLE_VIEWER);
        createAdmin("smoke_test", smokePassword, "冒烟测试账号", "SMOKE_TEST");
        createBusinessUser("biz01", businessPassword, "业务演示用户");
    }

    private void createAdmin(String username, String rawPassword, String nickname, String roleCode) {
        if (userMapper.selectCount(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, username)) > 0) {
            return;
        }
        SysRole role = roleMapper.selectOne(Wrappers.<SysRole>lambdaQuery().eq(SysRole::getCode, roleCode));
        if (role == null) {
            log.warn("角色 {} 不存在，跳过账号 {} 初始化", roleCode, username);
            return;
        }
        SysUser user = new SysUser()
                .setUsername(username)
                .setPassword(passwordEncoder.encode(rawPassword))
                .setNickname(nickname)
                .setStatus(Constants.STATUS_ENABLED);
        userMapper.insert(user);

        SysUserRole relation = new SysUserRole();
        relation.setUserId(user.getId());
        relation.setRoleId(role.getId());
        relation.setCreatedAt(LocalDateTime.now());
        userRoleMapper.insert(relation);
        log.info("已初始化后台演示账号: {}", username);
    }

    private void createBusinessUser(String username, String rawPassword, String nickname) {
        if (bizUserMapper.selectCount(Wrappers.<BizUser>lambdaQuery().eq(BizUser::getUsername, username)) > 0) {
            return;
        }
        BizUser user = new BizUser()
                .setUsername(username)
                .setPassword(passwordEncoder.encode(rawPassword))
                .setNickname(nickname)
                .setStatus(Constants.STATUS_ENABLED);
        bizUserMapper.insert(user);
        log.info("已初始化业务演示账号: {}", username);
    }
}
