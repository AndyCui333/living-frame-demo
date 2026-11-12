package com.livingframe.business.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.livingframe.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName("cs_ticket")
public class CsTicket extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String ticketNo;
    private Long portalUserId;
    private String title;
    private String content;
    private String ticketType;
    private String status;
    private Long assignedAgentId;
    private LocalDateTime assignedAt;
    private LocalDateTime lastMessageAt;
    private LocalDateTime closedAt;
    private Integer rating;
    private String feedback;
}
