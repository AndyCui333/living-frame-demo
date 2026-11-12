package com.livingframe.system.dto;

import com.livingframe.common.api.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class LoginLogQuery extends PageQuery {

    private String username;
    private String ip;
    private String status;
    private String loginType;
}
