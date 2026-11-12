package com.livingframe.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateBucketRequest(
        @NotBlank(message = "Bucket 名称不能为空")
        @Size(min = 3, max = 63, message = "Bucket 名称长度需要 3-63 个字符")
        @Pattern(regexp = "^[a-z0-9][a-z0-9.-]*[a-z0-9]$",
                message = "Bucket 只能包含小写字母、数字、点和短横线，且必须以字母或数字开头结尾")
        String bucketName
) {
}
