package com.livingframe.framework.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.redisson")
public class RedissonProperties {

    private String address;
    private String password;
    private int database = 0;
}
