package com.livingframe.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(
        scanBasePackages = "com.livingframe",
        exclude = UserDetailsServiceAutoConfiguration.class)
@MapperScan({"com.livingframe.system.mapper", "com.livingframe.business.mapper", "com.livingframe.ai.mapper"})
public class LivingFrameApplication {

    public static void main(String[] args) {
        System.setProperty("io.netty.noUnsafe", "true");
        SpringApplication.run(LivingFrameApplication.class, args);
    }
}
