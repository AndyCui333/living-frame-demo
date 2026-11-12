package com.livingframe.ai.service;

import com.livingframe.ai.config.AiMemoryProperties;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 轻量本地 embedding：中文单字/双字 + 英文/数字 token 的哈希向量。
 * 不依赖额外的 embedding 模型，适合作为对话长期记忆的本地召回方案。
 */
@Service
public class AiEmbeddingService {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("[\\p{IsHan}]|[a-z0-9_]+");

    private final int dimension;

    public AiEmbeddingService(AiMemoryProperties properties) {
        this.dimension = properties.getVectorSize();
    }

    public List<Float> embed(String text) {
        float[] vector = new float[dimension];
        if (text == null || text.isBlank()) {
            vector[0] = 1.0f;
            return toList(vector);
        }
        String normalized = text.toLowerCase();
        Matcher matcher = TOKEN_PATTERN.matcher(normalized);
        List<String> tokens = new ArrayList<>();
        String previousHan = null;
        while (matcher.find()) {
            String token = matcher.group();
            tokens.add(token);
            boolean han = token.length() == 1 && Character.UnicodeScript.of(token.charAt(0)) == Character.UnicodeScript.HAN;
            if (han) {
                if (previousHan != null) {
                    tokens.add(previousHan + token);
                }
                previousHan = token;
            } else {
                previousHan = null;
            }
        }
        int used = 0;
        for (String token : tokens) {
            int hash = token.hashCode();
            int index = Math.floorMod(hash, dimension);
            float sign = ((hash >>> 16) & 1) == 0 ? 1.0f : -1.0f;
            vector[index] += sign;
            used++;
        }
        if (used == 0) {
            vector[0] = 1.0f;
        }
        double norm = 0;
        for (float value : vector) {
            norm += value * value;
        }
        if (norm > 0) {
            float factor = (float) (1.0 / Math.sqrt(norm));
            for (int i = 0; i < vector.length; i++) {
                vector[i] *= factor;
            }
        }
        return toList(vector);
    }

    private List<Float> toList(float[] vector) {
        List<Float> values = new ArrayList<>(vector.length);
        for (float value : vector) {
            values.add(value);
        }
        return values;
    }
}
