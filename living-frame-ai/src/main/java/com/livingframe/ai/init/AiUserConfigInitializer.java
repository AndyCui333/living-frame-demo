package com.livingframe.ai.init;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.livingframe.ai.mapper.SysUserAiConfigMapper;
import com.livingframe.ai.service.AiUserConfigService;
import com.livingframe.system.entity.SysUser;
import com.livingframe.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 为已有账号补齐三个默认 AI 模型配置；新账号通过 UserCreatedEvent 初始化。
 */
@Slf4j
@Component
@Order(20)
@RequiredArgsConstructor
public class AiUserConfigInitializer implements ApplicationRunner {

    private final SysUserMapper userMapper;
    private final AiUserConfigService configService;
    private final SysUserAiConfigMapper configMapper;

    @Override
    public void run(ApplicationArguments args) {
        // 只在表完全为空时补齐历史账号；用户主动删除默认模型后不会在每次重启时被重新添加
        Long count = configMapper.selectCount(null);
        if (count != null && count > 0) {
            return;
        }
        for (SysUser user : userMapper.selectList(Wrappers.<SysUser>lambdaQuery())) {
            try {
                configService.ensureDefaults(user.getId());
            } catch (Exception ex) {
                log.warn("初始化用户 {} 的 AI 默认配置失败: {}", user.getUsername(), ex.getMessage());
            }
        }
    }
}
