package com.livingframe.ai.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.livingframe.ai.dto.AiStatusResponse;
import com.livingframe.ai.dto.AiUserConfigRequest;
import com.livingframe.ai.dto.AiUserConfigView;
import com.livingframe.ai.entity.SysUserAiConfig;
import com.livingframe.ai.mapper.SysUserAiConfigMapper;
import com.livingframe.ai.model.AiResolvedConfig;
import com.livingframe.common.context.LoginUserContext;
import com.livingframe.common.event.UserCreatedEvent;
import com.livingframe.common.event.UserDeletedEvent;
import com.livingframe.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiUserConfigService {

    private static final BigDecimal DEFAULT_TEMPERATURE = new BigDecimal("0.70");
    private static final int DEFAULT_MAX_TOKENS = 4096;

    private static final List<DefaultModel> DEFAULT_MODELS = List.of(
            new DefaultModel("DEEPSEEK", "DeepSeek", "https://api.deepseek.com", "deepseek-flash"),
            new DefaultModel("QWEN", "通义千问", "https://dashscope.aliyuncs.com/compatible-mode", "qwen3.8-max"),
            new DefaultModel("OPENAI", "OpenAI", "https://api.openai.com", "gpt-4o")
    );

    private final SysUserAiConfigMapper configMapper;
    private final AiKeyCipher keyCipher;

    public List<AiUserConfigView> listCurrent() {
        return configMapper.selectList(Wrappers.<SysUserAiConfig>lambdaQuery()
                        .eq(SysUserAiConfig::getUserId, LoginUserContext.require().getId())
                        .orderByDesc(SysUserAiConfig::getIsDefault)
                        .orderByDesc(SysUserAiConfig::getId))
                .stream().map(this::toView).toList();
    }

    public AiStatusResponse status() {
        SysUserAiConfig config = findDefault(LoginUserContext.require().getId());
        if (config == null) {
            return new AiStatusResponse(false, "", "", "", "", "");
        }
        boolean hasKey = keyCipher.hasKey(config.getApiKeyEncrypted())
                && StrUtil.isNotBlank(keyCipher.decrypt(config.getApiKeyEncrypted()));
        return new AiStatusResponse(
                hasKey,
                config.getProvider(),
                config.getProviderName(),
                config.getModel(),
                config.getModel(),
                config.getReasoningLevel());
    }

    @Transactional(rollbackFor = Exception.class)
    public void ensureDefaults(Long userId) {
        Long count = configMapper.selectCount(Wrappers.<SysUserAiConfig>lambdaQuery()
                .eq(SysUserAiConfig::getUserId, userId));
        if (count != null && count > 0) {
            return;
        }
        boolean first = true;
        for (DefaultModel model : DEFAULT_MODELS) {
            configMapper.insert(new SysUserAiConfig()
                    .setUserId(userId)
                    .setProvider(model.provider())
                    .setProviderName(model.providerName())
                    .setBaseUrl(model.baseUrl())
                    .setModel(model.model())
                    .setApiKeyEncrypted("")
                    .setReasoningLevel("HIGH")
                    .setTemperature(DEFAULT_TEMPERATURE)
                    .setMaxTokens(DEFAULT_MAX_TOKENS)
                    .setIsDefault(first ? 1 : 0));
            first = false;
        }
    }

    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void onUserCreated(UserCreatedEvent event) {
        try {
            ensureDefaults(event.userId());
        } catch (Exception ex) {
            log.error("初始化用户 AI 默认模型失败: userId={}", event.userId(), ex);
        }
    }

    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void onUserDeleted(UserDeletedEvent event) {
        try {
            configMapper.deleteByUserId(event.userId());
        } catch (Exception ex) {
            log.error("清理用户 AI 配置失败: userId={}", event.userId(), ex);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(AiUserConfigRequest request) {
        Long userId = LoginUserContext.require().getId();
        ensureDuplicate(userId, request.provider(), request.model(), null);
        boolean first = configMapper.selectCount(Wrappers.<SysUserAiConfig>lambdaQuery()
                .eq(SysUserAiConfig::getUserId, userId)) == 0;
        boolean makeDefault = first || Boolean.TRUE.equals(request.defaultConfig());
        if (makeDefault) {
            clearDefault(userId);
        }
        SysUserAiConfig config = new SysUserAiConfig()
                .setUserId(userId)
                .setProvider(request.provider())
                .setProviderName(request.providerName().trim())
                .setBaseUrl(normalizeBaseUrl(request.baseUrl()))
                .setModel(request.model().trim())
                .setApiKeyEncrypted(keyCipher.encrypt(StrUtil.trimToEmpty(request.apiKey())))
                .setReasoningLevel(StrUtil.blankToDefault(request.reasoningLevel(), "HIGH"))
                .setTemperature(request.temperature() == null ? DEFAULT_TEMPERATURE : request.temperature())
                .setMaxTokens(request.maxTokens() == null ? DEFAULT_MAX_TOKENS : request.maxTokens())
                .setIsDefault(makeDefault ? 1 : 0);
        configMapper.insert(config);
        return config.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, AiUserConfigRequest request) {
        SysUserAiConfig config = requireOwned(id);
        ensureDuplicate(config.getUserId(), request.provider(), request.model(), id);
        config.setProvider(request.provider());
        config.setProviderName(request.providerName().trim());
        config.setBaseUrl(normalizeBaseUrl(request.baseUrl()));
        config.setModel(request.model().trim());
        if (Boolean.TRUE.equals(request.clearApiKey())) {
            config.setApiKeyEncrypted("");
        } else if (StrUtil.isNotBlank(request.apiKey())) {
            config.setApiKeyEncrypted(keyCipher.encrypt(request.apiKey().trim()));
        }
        config.setReasoningLevel(StrUtil.blankToDefault(request.reasoningLevel(), "HIGH"));
        config.setTemperature(request.temperature() == null ? DEFAULT_TEMPERATURE : request.temperature());
        config.setMaxTokens(request.maxTokens() == null ? DEFAULT_MAX_TOKENS : request.maxTokens());
        if (Boolean.TRUE.equals(request.defaultConfig())) {
            clearDefault(config.getUserId());
            config.setIsDefault(1);
        }
        configMapper.updateById(config);
    }

    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long id) {
        SysUserAiConfig config = requireOwned(id);
        clearDefault(config.getUserId());
        config.setIsDefault(1);
        configMapper.updateById(config);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysUserAiConfig config = requireOwned(id);
        boolean wasDefault = Integer.valueOf(1).equals(config.getIsDefault());
        configMapper.deletePhysical(id);
        if (wasDefault) {
            SysUserAiConfig next = findDefault(config.getUserId());
            if (next == null) {
                List<SysUserAiConfig> remaining = configMapper.selectList(Wrappers.<SysUserAiConfig>lambdaQuery()
                        .eq(SysUserAiConfig::getUserId, config.getUserId())
                        .orderByAsc(SysUserAiConfig::getId));
                if (!remaining.isEmpty()) {
                    SysUserAiConfig promoted = remaining.get(0);
                    promoted.setIsDefault(1);
                    configMapper.updateById(promoted);
                }
            }
        }
    }

    public AiResolvedConfig resolveCurrent() {
        SysUserAiConfig config = findDefault(LoginUserContext.require().getId());
        if (config == null) {
            throw new BusinessException("当前账号尚未配置 AI 模型，请到「AI 设置」添加模型并填写 API Key");
        }
        String apiKey = keyCipher.decrypt(config.getApiKeyEncrypted());
        if (StrUtil.isBlank(apiKey)) {
            throw new BusinessException("当前模型尚未配置 API Key，请到「AI 设置」填写");
        }
        return new AiResolvedConfig(
                config.getProvider(),
                config.getProviderName(),
                config.getModel(),
                config.getReasoningLevel(),
                config.getTemperature(),
                config.getMaxTokens(),
                config.getBaseUrl(),
                apiKey,
                true);
    }

    private SysUserAiConfig findDefault(Long userId) {
        return configMapper.selectOne(Wrappers.<SysUserAiConfig>lambdaQuery()
                .eq(SysUserAiConfig::getUserId, userId)
                .eq(SysUserAiConfig::getIsDefault, 1)
                .orderByDesc(SysUserAiConfig::getId)
                .last("LIMIT 1"));
    }

    private void ensureDuplicate(Long userId, String provider, String model, Long excludeId) {
        var wrapper = Wrappers.<SysUserAiConfig>lambdaQuery()
                .eq(SysUserAiConfig::getUserId, userId)
                .eq(SysUserAiConfig::getProvider, provider)
                .eq(SysUserAiConfig::getModel, model.trim());
        if (excludeId != null) {
            wrapper.ne(SysUserAiConfig::getId, excludeId);
        }
        if (configMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("该账号已存在相同厂商和模型的配置");
        }
    }

    private SysUserAiConfig requireOwned(Long id) {
        SysUserAiConfig config = configMapper.selectById(id);
        if (config == null || !config.getUserId().equals(LoginUserContext.require().getId())) {
            throw new BusinessException("AI 模型配置不存在");
        }
        return config;
    }

    private void clearDefault(Long userId) {
        configMapper.update(null, Wrappers.<SysUserAiConfig>lambdaUpdate()
                .eq(SysUserAiConfig::getUserId, userId)
                .eq(SysUserAiConfig::getIsDefault, 1)
                .set(SysUserAiConfig::getIsDefault, 0));
    }

    private String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl.trim().replaceAll("/+$", "");
        if (normalized.endsWith("/v1")) {
            normalized = normalized.substring(0, normalized.length() - 3);
        }
        return normalized;
    }

    private AiUserConfigView toView(SysUserAiConfig config) {
        return new AiUserConfigView(
                config.getId(),
                config.getProvider(),
                config.getProviderName(),
                config.getBaseUrl(),
                config.getModel(),
                keyCipher.hasKey(config.getApiKeyEncrypted()),
                Integer.valueOf(1).equals(config.getIsDefault()),
                config.getReasoningLevel(),
                config.getTemperature(),
                config.getMaxTokens());
    }

    private record DefaultModel(String provider, String providerName, String baseUrl, String model) {
    }
}
