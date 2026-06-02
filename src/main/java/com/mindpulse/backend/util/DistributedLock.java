package com.mindpulse.backend.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

/**
 * Redis 分布式锁，基于 SETNX + 过期时间 + Lua 脚本安全解锁
 * 解决多实例部署下的并发冲突问题
 */
@Component
public class DistributedLock {

    private static final Logger log = LoggerFactory.getLogger(DistributedLock.class);

    private static final String LOCK_PREFIX = "lock:";
    private static final long DEFAULT_LOCK_TTL = 10; // 默认锁过期时间（秒）

    /**
     * Lua 脚本：仅当 value 匹配时才删除 key，防止误删其他实例的锁
     */
    private static final String UNLOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "    return redis.call('del', KEYS[1]) " +
            "else " +
            "    return 0 " +
            "end";

    private static final DefaultRedisScript<Long> REDIS_SCRIPT;

    static {
        REDIS_SCRIPT = new DefaultRedisScript<>();
        REDIS_SCRIPT.setScriptText(UNLOCK_SCRIPT);
        REDIS_SCRIPT.setResultType(Long.class);
    }

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private final String lockId = UUID.randomUUID().toString();

    /**
     * 尝试获取分布式锁
     *
     * @param key       锁标识（业务维度，如 task:123）
     * @param ttlSeconds 锁过期时间（秒），超过后自动释放，防止死锁
     * @return 锁令牌（用于解锁），获取失败返回 null
     */
    public String tryLock(String key, long ttlSeconds) {
        String lockKey = LOCK_PREFIX + key;
        String lockValue = lockId + ":" + Thread.currentThread().getId();
        try {
            Boolean acquired = stringRedisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, Duration.ofSeconds(ttlSeconds));
            if (Boolean.TRUE.equals(acquired)) {
                log.debug("分布式锁获取成功: key={}", lockKey);
                return lockValue;
            }
            log.debug("分布式锁获取失败（已被占用）: key={}", lockKey);
            return null;
        } catch (Exception e) {
            log.error("分布式锁操作异常: key={}, error={}", lockKey, e.getMessage());
            return null;
        }
    }

    public String tryLock(String key) {
        return tryLock(key, DEFAULT_LOCK_TTL);
    }

    /**
     * 释放分布式锁（仅释放自己持有的锁）
     *
     * @param key       锁标识
     * @param lockValue 获取锁时返回的令牌
     * @return true 释放成功，false 锁已过期或被其他实例持有
     */
    public boolean unlock(String key, String lockValue) {
        if (lockValue == null) {
            return false;
        }
        String lockKey = LOCK_PREFIX + key;
        try {
            Long result = stringRedisTemplate.execute(
                    REDIS_SCRIPT,
                    Collections.singletonList(lockKey),
                    lockValue
            );
            boolean released = result != null && result > 0;
            if (released) {
                log.debug("分布式锁释放成功: key={}", lockKey);
            } else {
                log.debug("分布式锁释放失败（已过期或不属己）: key={}", lockKey);
            }
            return released;
        } catch (Exception e) {
            log.error("分布式锁释放异常: key={}, error={}", lockKey, e.getMessage());
            return false;
        }
    }

    /**
     * 带锁执行任务（自动获取和释放）
     *
     * @param key       锁标识
     * @param ttlSeconds 锁过期时间
     * @param action    需要原子执行的操作
     * @return true 操作成功，false 未获取到锁
     */
    public boolean executeWithLock(String key, long ttlSeconds, Runnable action) {
        String lockValue = tryLock(key, ttlSeconds);
        if (lockValue == null) {
            log.warn("未能获取分布式锁，操作取消: key={}", key);
            return false;
        }
        try {
            action.run();
            return true;
        } finally {
            unlock(key, lockValue);
        }
    }

    public boolean executeWithLock(String key, Runnable action) {
        return executeWithLock(key, DEFAULT_LOCK_TTL, action);
    }
}
