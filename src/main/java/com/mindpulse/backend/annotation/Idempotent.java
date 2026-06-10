package com.mindpulse.backend.annotation;

import java.lang.annotation.*;

/**
 * Idempotency annotation for controller methods.
 * Prevents duplicate requests by hashing (userId + url + body) and storing in Redis with SETNX.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {
    int expireSeconds() default 300;
}
