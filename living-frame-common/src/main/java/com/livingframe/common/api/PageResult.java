package com.livingframe.common.api;

import java.io.Serializable;
import java.util.List;

/**
 * 分页结果。
 */
public record PageResult<T>(List<T> records, long total, long pageNum, long pageSize) implements Serializable {

    public static <T> PageResult<T> of(List<T> records, long total, long pageNum, long pageSize) {
        return new PageResult<>(records, total, pageNum, pageSize);
    }

    public static <T> PageResult<T> empty(long pageNum, long pageSize) {
        return new PageResult<>(List.of(), 0L, pageNum, pageSize);
    }
}
