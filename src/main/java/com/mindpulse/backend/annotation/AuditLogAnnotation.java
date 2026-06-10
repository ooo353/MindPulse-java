package com.mindpulse.backend.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLogAnnotation {
    String action();
    String resourceType();
}
