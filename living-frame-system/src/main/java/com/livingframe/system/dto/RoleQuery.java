package com.livingframe.system.dto;

import com.livingframe.common.api.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RoleQuery extends PageQuery {

    private Integer status;
}
