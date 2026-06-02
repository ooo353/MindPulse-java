package com.mindpulse.backend.service.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SemanticCacheServiceTest {

    private SemanticCacheService service;

    @BeforeEach
    void setUp() {
        // Use null RedisTemplate — only testing normalize and hash logic
        service = new SemanticCacheService(null);
    }

    @Test
    @DisplayName("归一化应去除标点符号")
    void normalizeShouldRemovePunctuation() {
        String input = "明天下午3点，去图书馆还书！优先级高";
        String result = service.normalize(input);
        assertFalse(result.contains("，"));
        assertFalse(result.contains("！"));
    }

    @Test
    @DisplayName("归一化应转小写")
    void normalizeShouldLowercase() {
        String input = "IMPORTANT Task Description";
        String result = service.normalize(input);
        assertEquals("importanttaskdescription", result);
    }

    @Test
    @DisplayName("同义词 '明天' 应替换为 '次日'")
    void normalizeShouldReplaceSynonyms() {
        String input = "明天下午去开会";
        String result = service.normalize(input);
        assertTrue(result.contains("次日"));
        assertFalse(result.contains("明天"));
    }

    @Test
    @DisplayName("同义词 '优先级高' 应替换")
    void normalizeShouldReplacePrioritySynonyms() {
        String input = "完成任务优先级高";
        String result = service.normalize(input);
        assertTrue(result.contains("高优先级"));
    }

    @Test
    @DisplayName("null 输入应返回空字符串")
    void normalizeNullShouldReturnEmpty() {
        assertEquals("", service.normalize(null));
    }

    @Test
    @DisplayName("空白输入应返回空字符串")
    void normalizeBlankShouldReturnEmpty() {
        assertEquals("", service.normalize("   "));
    }

    @Test
    @DisplayName("相同语义的输入应生成相同哈希")
    void sameSemanticsShouldProduceSameHash() {
        String text1 = "明天下午3点提醒我去图书馆还书，优先级高";
        String text2 = "明天 下午3点 提醒我 去图书馆 还书 优先级高";

        String norm1 = service.normalize(text1);
        String norm2 = service.normalize(text2);

        assertEquals(norm1, norm2);
        assertEquals(service.hash(norm1), service.hash(norm2));
    }

    @Test
    @DisplayName("SHA-256 哈希应为 64 位十六进制字符串")
    void hashShouldBe64CharsHex() {
        String hash = service.hash("test");
        assertEquals(64, hash.length());
        assertTrue(hash.matches("[0-9a-f]+"));
    }

    @Test
    @DisplayName("getCacheStats 应返回初始零值")
    void cacheStatsShouldStartAtZero() {
        var stats = service.getCacheStats();
        assertEquals(0, stats.getTotalRequests());
        assertEquals(0, stats.getCacheHits());
        assertEquals(0, stats.getCacheMisses());
        assertEquals(0.0, stats.getHitRate());
    }
}
