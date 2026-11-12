package com.livingframe.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.livingframe.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("ai_message")
public class AiMessage extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long conversationId;
    private Long userId;
    private String role;
    private String content;
    private String model;
}
