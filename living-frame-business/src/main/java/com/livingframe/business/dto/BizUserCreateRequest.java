package com.livingframe.business.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BizUserCreateRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 4, max = 32, message = "用户名长度需为 4-32 位")
        @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
        String username,

        @NotBlank(message = "密码不能为空")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,32}$",
                message = "密码需为 8-32 位，且同时包含字母和数字")
        String password,

        @Size(max = 64, message = "昵称长度不能超过 64")
        String nickname,

        @Size(max = 32, message = "手机号长度不能超过 32")
        String phone,

        @Email(message = "邮箱格式不正确")
        @Size(max = 128, message = "邮箱长度不能超过 128")
        String email,

        @Size(max = 255, message = "头像地址过长")
        String avatar
) {
}
