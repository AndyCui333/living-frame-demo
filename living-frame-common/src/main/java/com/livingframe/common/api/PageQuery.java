package com.livingframe.common.api;

import lombok.Data;

/**
 * 通用分页查询参数。
 */
@Data
public class PageQuery {

    private long pageNum = 1L;
    private long pageSize = 10L;
    private String keyword;

    public void normalize() {
        if (pageNum < 1) {
            pageNum = 1L;
        }
        if (pageSize < 1) {
            pageSize = 10L;
        }
        if (pageSize > 100) {
            pageSize = 100L;
        }
    }
}
