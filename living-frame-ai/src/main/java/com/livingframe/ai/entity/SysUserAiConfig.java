package com.livingframe.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.livingframe.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user_ai_config")
public class SysUserAiConfig extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;
    private String provider;
    private String providerName;
    private String baseUrl;
    private String model;
    private String apiKeyEncrypted;
    private String reasoningLevel;
    private BigDecimal temperature;
    private Integer maxTokens;
    private Integer isDefault;
}
