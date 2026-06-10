package com.mindpulse.backend.aspect;

import com.mindpulse.backend.annotation.AuditLogAnnotation;
import com.mindpulse.backend.entity.AuditLog;
import com.mindpulse.backend.service.IAuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final IAuditLogService auditLogService;

    @Around("@annotation(auditLogAnnotation)")
    public Object logAudit(ProceedingJoinPoint pjp, AuditLogAnnotation auditLogAnnotation) throws Throwable {
        boolean success = false;
        try {
            Object result = pjp.proceed();
            success = true;
            return result;
        } catch (Throwable t) {
            throw t;
        } finally {
            try {
                var authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null) {
                String userId = authentication.getName();
                HttpServletRequest request = getCurrentRequest();

                AuditLog auditLog = new AuditLog();
                auditLog.setUserId(userId);
                auditLog.setAction(auditLogAnnotation.action());
                auditLog.setResourceType(auditLogAnnotation.resourceType());

                if (request != null) {
                    auditLog.setIpAddress(request.getRemoteAddr());
                    auditLog.setUserAgent(request.getHeader("User-Agent"));
                }

                String methodName = pjp.getSignature().getName();
                auditLog.setDetails("Method: " + methodName + ", Success: " + success);
                auditLog.setCreatedAt(LocalDateTime.now());

                // Extract resourceId from Long parameters
                for (Object arg : pjp.getArgs()) {
                    if (arg instanceof Long longArg) {
                        auditLog.setResourceId(longArg);
                        break;
                    }
                }

                auditLogService.saveAuditLog(auditLog);
                }
            } catch (Exception e) {
                log.error("Failed to create audit log", e);
            }
        }
    }

    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attrs != null ? attrs.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
