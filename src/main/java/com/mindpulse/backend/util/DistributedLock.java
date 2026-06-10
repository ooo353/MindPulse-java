package com.mindpulse.backend.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

/**
 * Redis distributed lock based on SETNX + TTL + Lua script safe unlock.
 * Resolves concurrency conflicts in multi-instance deployments.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DistributedLock {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String LOCK_PREFIX = "lock:";
    private static final long DEFAULT_LOCK_TTL = 10;

    // Lua script: only delete key when value matches, preventing accidental deletion of other instances' locks
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

    private final String lockId = UUID.randomUUID().toString();

    /**
     * Try to acquire distributed lock
     *
     * @param key        lock identifier (business dimension, e.g. task:123)
     * @param ttlSeconds lock expiration in seconds, auto-releases after timeout to prevent deadlocks
     * @return lock token (for unlock), null if acquisition failed
     */
    public String tryLock(String key, long ttlSeconds) {
        String lockKey = LOCK_PREFIX + key;
        String lockValue = lockId + ":" + Thread.currentThread().getId();
        try {
            Boolean acquired = stringRedisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, Duration.ofSeconds(ttlSeconds));
            if (Boolean.TRUE.equals(acquired)) {
                log.debug("Distributed lock acquired: key={}", lockKey);
                return lockValue;
            }
            log.debug("Distributed lock acquisition failed (already held): key={}", lockKey);
            return null;
        } catch (Exception e) {
            log.error("Distributed lock operation exception: key={}, error={}", lockKey, e.getMessage());
            return null;
        }
    }

    public String tryLock(String key) {
        return tryLock(key, DEFAULT_LOCK_TTL);
    }

    /**
     * Release distributed lock (only releases locks held by this instance)
     *
     * @param key       lock identifier
     * @param lockValue token returned when acquiring the lock
     * @return true if released successfully, false if lock expired or held by another instance
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
                log.debug("Distributed lock released: key={}", lockKey);
            } else {
                log.debug("Distributed lock release failed (expired or not owned): key={}", lockKey);
            }
            return released;
        } catch (Exception e) {
            log.error("Distributed lock release exception: key={}, error={}", lockKey, e.getMessage());
            return false;
        }
    }

    /**
     * Execute task with lock (auto-acquire and release)
     *
     * @param key        lock identifier
     * @param ttlSeconds lock expiration
     * @param action     operation to execute atomically
     * @return true if operation succeeded, false if lock not acquired
     */
    public boolean executeWithLock(String key, long ttlSeconds, Runnable action) {
        String lockValue = tryLock(key, ttlSeconds);
        if (lockValue == null) {
            log.warn("Failed to acquire distributed lock, operation cancelled: key={}", key);
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
