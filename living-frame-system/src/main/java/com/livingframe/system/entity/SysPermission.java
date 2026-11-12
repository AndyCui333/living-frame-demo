package com.livingframe.system.entity;

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
@TableName("sys_permission")
public class SysPermission extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String code;
    private String name;
    /** MENU / BUTTON。 */
    private String type;
    private Long parentId;
    private String path;
    private String component;
    private String icon;
    private Integer sort;
    /** 1 启用，0 禁用。 */
    private Integer status;
}
