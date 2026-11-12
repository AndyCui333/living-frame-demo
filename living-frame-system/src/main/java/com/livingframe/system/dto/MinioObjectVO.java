package com.livingframe.system.dto;

public record MinioObjectVO(
        String objectName,
        String name,
        long size,
        String lastModified,
        boolean dir
) {
}
