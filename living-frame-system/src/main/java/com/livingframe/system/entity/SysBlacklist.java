package com.livingframe.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@TableName("sys_blacklist")
public class SysBlacklist {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String targetType;
    private String targetValue;
    private String reason;
    private Integer status;
    private String createdBy;
}
