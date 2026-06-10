package com.mindpulse.backend.annotation;

import java.lang.annotation.*;

/**
 * Rate limiting annotation for controller methods.
 * Tracks request count per user per endpoint within a sliding window using Redis INCR + TTL.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    int maxRequests() default 100;
    int windowSeconds() default 60;
}
