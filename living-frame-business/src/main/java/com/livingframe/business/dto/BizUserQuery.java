package com.livingframe.business.dto;

import com.livingframe.common.api.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class BizUserQuery extends PageQuery {

    private Integer status;
}
