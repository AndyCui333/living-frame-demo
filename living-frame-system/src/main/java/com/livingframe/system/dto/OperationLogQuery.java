package com.livingframe.system.dto;

import com.livingframe.common.api.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OperationLogQuery extends PageQuery {

    private String username;
    private String operation;
    private String status;
}
