package com.mindpulse.backend.controller;

import com.mindpulse.backend.annotation.AuditLogAnnotation;
import com.mindpulse.backend.annotation.RateLimit;
import com.mindpulse.backend.dto.ApiResponse;
import com.mindpulse.backend.dto.LoginDto;
import com.mindpulse.backend.dto.UserDto;
import com.mindpulse.backend.entity.User;
import com.mindpulse.backend.security.CustomUserDetails;
import com.mindpulse.backend.security.JwtUtil;
import com.mindpulse.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证管理", description = "用户注册和登录认证接口")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    @Operation(summary = "用户注册")
    @AuditLogAnnotation(action = "REGISTER", resourceType = "USER")
    @RateLimit(maxRequests = 10, windowSeconds = 60)
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> registerUser(@Valid @RequestBody UserDto userDto) {
        log.info("Registration attempt for username: {}", userDto.username());
        try {
            User user = userService.registerUser(userDto);
            Map<String, Object> data = new HashMap<>();
            data.put("username", user.getUsername());
            data.put("email", user.getEmail());
            log.info("User registered successfully: {}", user.getUsername());
            return ResponseEntity.ok(ApiResponse.success("User registered successfully", data));
        } catch (RuntimeException e) {
            log.warn("Registration failed for username {}: {}", userDto.username(), e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    @Operation(summary = "用户登录")
    @RateLimit(maxRequests = 10, windowSeconds = 60)
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> loginUser(@Valid @RequestBody LoginDto loginDto) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginDto.username(),
                            loginDto.password()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String jwt = jwtUtil.generateToken(authentication.getName());

            Map<String, Object> data = new HashMap<>();
            data.put("token", jwt);
            data.put("username", userDetails.getUsername());
            data.put("email", userDetails.getEmail());
            data.put("role", userDetails.getRole());

            log.info("User logged in successfully: {}", userDetails.getUsername());
            return ResponseEntity.ok(ApiResponse.success("Login successful", data));
        } catch (Exception e) {
            log.warn("Failed login attempt for username: {}", loginDto.username());
            return ResponseEntity.status(401).body(ApiResponse.unauthorized("Invalid username or password"));
        }
    }
}
