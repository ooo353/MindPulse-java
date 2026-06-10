package com.mindpulse.backend.interceptor;

import com.mindpulse.backend.annotation.RateLimit;
import com.mindpulse.backend.exception.TooManyRequestsException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
        if (rateLimit == null) {
            return true;
        }

        String userId = getCurrentUsername();
        String endpoint = request.getRequestURI();
        String key = "rate:" + userId + ":" + endpoint;

        Long count = incrementWithTtl(key, rateLimit.windowSeconds());

        if (count != null && count > rateLimit.maxRequests()) {
            throw new TooManyRequestsException(
                    "Rate limit exceeded. Max " + rateLimit.maxRequests()
                            + " requests per " + rateLimit.windowSeconds() + " seconds.");
        }

        return true;
    }

    private Long incrementWithTtl(String key, long ttlSeconds) {
        String luaScript = """
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return count
            """;
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(luaScript, Long.class);
        return stringRedisTemplate.execute(redisScript, List.of(key), String.valueOf(ttlSeconds));
    }

    private String getCurrentUsername() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            return "anonymous";
        }
    }
}
