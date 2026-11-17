package com.livingframe.system.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.minio")
public class MinioProperties {

    private String endpoint = "http://192.168.28.212:9510";
    private String accessKey = "admin";
    private String secretKey = "Minio@123456";
    private String defaultBucket = "living-frame";
    private long presignedExpirySeconds = 3600;
}
