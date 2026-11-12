package com.livingframe.system.service;

import cn.hutool.core.util.IdUtil;
import com.livingframe.common.constant.Constants;
import com.livingframe.common.exception.BusinessException;
import com.livingframe.system.vo.CaptchaResponse;
import com.wf.captcha.SpecCaptcha;
import com.wf.captcha.base.Captcha;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 图形验证码服务，验证码存放在 Redis，默认 1 分钟有效且一次性使用。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${app.security.captcha-enabled:true}")
    private boolean captchaEnabled;

    public CaptchaResponse generate() {
        if (!captchaEnabled) {
            return new CaptchaResponse("disabled", "", 0);
        }
        SpecCaptcha captcha = new SpecCaptcha(130, 48, 4);
        captcha.setCharType(Captcha.TYPE_DEFAULT);
        String code = captcha.text();
        String captchaId = IdUtil.fastSimpleUUID();
        stringRedisTemplate.opsForValue().set(
                Constants.CAPTCHA_KEY_PREFIX + captchaId,
                code,
                Duration.ofSeconds(Constants.CAPTCHA_TTL_SECONDS));
        try {
            return new CaptchaResponse(captchaId, captcha.toBase64(), Constants.CAPTCHA_TTL_SECONDS);
        } catch (Exception ex) {
            log.error("生成验证码失败", ex);
            throw new BusinessException("生成验证码失败");
        }
    }

    public void validate(String captchaId, String captchaCode) {
        if (!captchaEnabled) {
            return;
        }
        if (captchaId == null || captchaCode == null) {
            throw new BusinessException("验证码不能为空");
        }
        String key = Constants.CAPTCHA_KEY_PREFIX + captchaId;
        String expected = stringRedisTemplate.opsForValue().get(key);
        // 一次性使用：无论校验成功与否都删除
        stringRedisTemplate.delete(key);
        if (expected == null) {
            throw new BusinessException("验证码已过期，请重新获取");
        }
        if (!expected.equalsIgnoreCase(captchaCode.trim())) {
            throw new BusinessException("验证码错误");
        }
    }

    public boolean isCaptchaEnabled() {
        return captchaEnabled;
    }
}
