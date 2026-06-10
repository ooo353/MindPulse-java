package com.mindpulse.backend.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindpulse.backend.dto.CacheStatsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class SemanticCacheService {

    private static final String CACHE_KEY_PREFIX = "task:cache:";
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final AtomicLong totalCacheHitTime = new AtomicLong(0);
    private final AtomicLong totalCacheMissTime = new AtomicLong(0);

    private static final Map<String, String> SYNONYMS = new java.util.HashMap<>();
    static {
        SYNONYMS.put("tomorrow", "next day");
        SYNONYMS.put("today", "this day");
        SYNONYMS.put("yesterday", "previous day");
        SYNONYMS.put("urgent", "high priority");
    }

    private static final Pattern PUNCTUATION = Pattern.compile("[\\p{Punct}\\s]+");

    public String normalize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String normalized = PUNCTUATION.matcher(text).replaceAll("").toLowerCase();
        for (Map.Entry<String, String> entry : SYNONYMS.entrySet()) {
            normalized = normalized.replace(entry.getKey(), entry.getValue());
        }
        return normalized.trim();
    }

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

    @SuppressWarnings("unchecked")
    public Map<String, Object> get(String cacheKey, long startTimeMs) {
        totalRequests.incrementAndGet();
        try {
            Object cached = redisTemplate.opsForValue().get(CACHE_KEY_PREFIX + cacheKey);
            if (cached != null) {
                String json = cached.toString();
                if (json.startsWith("\"") && json.endsWith("\"")) {
                    json = json.substring(1, json.length() - 1).replace("\\\"", "\"");
                }
                Map<String, Object> result = objectMapper.readValue(json, Map.class);
                long elapsed = System.currentTimeMillis() - startTimeMs;
                cacheHits.incrementAndGet();
                totalCacheHitTime.addAndGet(elapsed);
                log.debug("Cache hit key={}, elapsed={}ms", cacheKey, elapsed);
                return result;
            }
        } catch (Exception e) {
            log.warn("Cache read exception: {}", e.getMessage());
        }
        cacheMisses.incrementAndGet();
        return null;
    }

    public void put(String cacheKey, Map<String, Object> data, long startTimeMs) {
        try {
            String json = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(CACHE_KEY_PREFIX + cacheKey, json, CACHE_TTL);
            long elapsed = System.currentTimeMillis() - startTimeMs;
            totalCacheMissTime.addAndGet(elapsed);
            log.debug("Cache write key={}, elapsed={}ms", cacheKey, elapsed);
        } catch (JsonProcessingException e) {
            log.warn("Cache serialization failed: {}", e.getMessage());
        }
    }

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
