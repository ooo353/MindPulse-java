package com.mindpulse.backend.interceptor;

import com.mindpulse.backend.annotation.Idempotent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class IdempotentInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        Idempotent idempotent = handlerMethod.getMethodAnnotation(Idempotent.class);
        if (idempotent == null) {
            return true;
        }

        String userId = getCurrentUsername();
        String url = request.getRequestURI();
        String body = readBody(request);
        String hash = sha256(userId + url + body);
        String key = "idempotent:" + hash;

        Boolean isNew = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, "1", Duration.ofSeconds(idempotent.expireSeconds()));
        if (Boolean.FALSE.equals(isNew)) {
            response.setStatus(409);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"code\":409,\"message\":\"Duplicate request detected\"}");
            return false;
        }

        return true;
    }

    // TODO: wrap request with ContentCachingRequestWrapper in a Filter to support body-based idempotency
    private String readBody(HttpServletRequest request) {
        try {
            if (request instanceof org.springframework.web.util.ContentCachingRequestWrapper wrapper) {
                return new String(wrapper.getContentAsByteArray(), request.getCharacterEncoding());
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private String getCurrentUsername() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            return "anonymous";
        }
    }
}
