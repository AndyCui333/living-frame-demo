package com.livingframe.system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @Size(max = 64, message = "昵称长度不能超过 64")
        String nickname,

        @Email(message = "邮箱格式不正确")
        @Size(max = 128, message = "邮箱长度不能超过 128")
        String email,

        @Size(max = 32, message = "手机号长度不能超过 32")
        String phone,

        @Size(max = 255, message = "头像地址过长")
        String avatar
) {
}
