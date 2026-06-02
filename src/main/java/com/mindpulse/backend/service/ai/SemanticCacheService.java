package com.mindpulse.backend.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindpulse.backend.dto.CacheStatsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

@Service
public class SemanticCacheService {

    private static final Logger log = LoggerFactory.getLogger(SemanticCacheService.class);
    private static final String CACHE_KEY_PREFIX = "task:cache:";
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final AtomicLong totalCacheHitTime = new AtomicLong(0);
    private final AtomicLong totalCacheMissTime = new AtomicLong(0);

    // 常见同义词映射
    private static final Map<String, String> SYNONYMS = new java.util.HashMap<>();
    static {
        SYNONYMS.put("明天", "次日");
        SYNONYMS.put("后天", "后日");
        SYNONYMS.put("今天", "今日");
        SYNONYMS.put("昨天", "昨日");
        SYNONYMS.put("下周一", "下周1");
        SYNONYMS.put("下周二", "下周2");
        SYNONYMS.put("下周三", "下周3");
        SYNONYMS.put("下周四", "下周4");
        SYNONYMS.put("下周五", "下周5");
        SYNONYMS.put("下周六", "下周6");
        SYNONYMS.put("下周日", "下周7");
        SYNONYMS.put("周一", "周1");
        SYNONYMS.put("周二", "周2");
        SYNONYMS.put("周三", "周3");
        SYNONYMS.put("周四", "周4");
        SYNONYMS.put("周五", "周5");
        SYNONYMS.put("周六", "周6");
        SYNONYMS.put("周日", "周7");
        SYNONYMS.put("优先级高", "高优先级");
        SYNONYMS.put("紧急", "高优先级");
    }

    private static final Pattern PUNCTUATION = Pattern.compile("[，。！？、；：“”‘’（）【】《》\\s]+");

    @Autowired
    public SemanticCacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 语义归一化处理
     */
    public String normalize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        // 去标点符号
        String normalized = PUNCTUATION.matcher(text).replaceAll("").toLowerCase();
        // 同义词替换
        for (Map.Entry<String, String> entry : SYNONYMS.entrySet()) {
            normalized = normalized.replace(entry.getKey(), entry.getValue());
        }
        return normalized.trim();
    }

    /**
     * 生成语义哈希
     */
    public String hash(String normalizedText) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalizedText.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * 查询缓存，命中返回解析结果，未命中返回 null
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> get(String cacheKey, long startTimeMs) {
        totalRequests.incrementAndGet();
        try {
            Object cached = redisTemplate.opsForValue().get(CACHE_KEY_PREFIX + cacheKey);
            if (cached != null) {
                String json = cached.toString();
                // 处理 GenericJackson2Json 序列化后可能多出引号的情况
                if (json.startsWith("\"") && json.endsWith("\"")) {
                    json = json.substring(1, json.length() - 1).replace("\\\"", "\"");
                }
                Map<String, Object> result = objectMapper.readValue(json, Map.class);
                long elapsed = System.currentTimeMillis() - startTimeMs;
                cacheHits.incrementAndGet();
                totalCacheHitTime.addAndGet(elapsed);
                log.debug("缓存命中 key={}, 耗时={}ms", cacheKey, elapsed);
                return result;
            }
        } catch (Exception e) {
            log.warn("缓存读取异常: {}", e.getMessage());
        }
        cacheMisses.incrementAndGet();
        return null;
    }

    /**
     * 写入缓存
     */
    public void put(String cacheKey, Map<String, Object> data, long startTimeMs) {
        try {
            String json = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(CACHE_KEY_PREFIX + cacheKey, json, CACHE_TTL);
            long elapsed = System.currentTimeMillis() - startTimeMs;
            totalCacheMissTime.addAndGet(elapsed);
            log.debug("缓存写入 key={}, 耗时={}ms", cacheKey, elapsed);
        } catch (JsonProcessingException e) {
            log.warn("缓存序列化失败: {}", e.getMessage());
        }
    }

    /**
     * 获取缓存统计信息
     */
    public CacheStatsResponse getCacheStats() {
        long total = totalRequests.get();
        long hits = cacheHits.get();
        long misses = cacheMisses.get();
        long hitTime = totalCacheHitTime.get();
        long missTime = totalCacheMissTime.get();

        CacheStatsResponse stats = new CacheStatsResponse();
        stats.setTotalRequests(total);
        stats.setCacheHits(hits);
        stats.setCacheMisses(misses);
        stats.setHitRate(total > 0 ? (double) hits / total : 0.0);
        stats.setAvgResponseTimeMs(total > 0 ? (hitTime + missTime) / total : 0);
        stats.setCacheHitAvgMs(hits > 0 ? hitTime / hits : 0);
        stats.setCacheMissAvgMs(misses > 0 ? missTime / misses : 0);
        return stats;
    }
}
