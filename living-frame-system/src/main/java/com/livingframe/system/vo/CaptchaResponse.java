package com.livingframe.system.vo;

public record CaptchaResponse(String captchaId, String image, long expiresIn) {
}
