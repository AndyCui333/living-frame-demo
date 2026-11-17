package com.livingframe.business.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BizProfileUpdateRequest(
        @NotBlank(message = "昵称不能为空")
        @Size(max = 64, message = "昵称长度不能超过 64")
        String nickname,

        @Size(max = 32, message = "手机号长度不能超过 32")
        String phone,

        @Pattern(regexp = "^$|^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message = "邮箱格式不正确")
        @Size(max = 128, message = "邮箱长度不能超过 128")
        String email,

        @Size(max = 255, message = "头像地址过长")
        String avatar
) {
}
