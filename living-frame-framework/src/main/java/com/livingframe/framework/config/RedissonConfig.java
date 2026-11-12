package com.livingframe.framework.config;

import cn.hutool.core.util.StrUtil;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(RedissonProperties.class)
public class RedissonConfig {

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(RedissonProperties properties) {
        Config config = new Config();
        config.useSingleServer()
                .setAddress(properties.getAddress())
                .setPassword(StrUtil.isBlank(properties.getPassword()) ? null : properties.getPassword())
                .setDatabase(properties.getDatabase())
                .setConnectionMinimumIdleSize(1)
                .setConnectionPoolSize(8)
                .setConnectTimeout((int) Duration.ofSeconds(5).toMillis())
                .setTimeout((int) Duration.ofSeconds(3).toMillis());
        return Redisson.create(config);
    }
}
