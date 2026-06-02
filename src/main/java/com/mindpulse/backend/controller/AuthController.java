package com.mindpulse.backend.controller;

import com.mindpulse.backend.dto.ApiResponse;
import com.mindpulse.backend.dto.UserDto;
import com.mindpulse.backend.entity.User;
import com.mindpulse.backend.security.CustomUserDetails;
import com.mindpulse.backend.security.JwtUtil;
import com.mindpulse.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> registerUser(@RequestBody UserDto userDto) {
        try {
            User user = userService.registerUser(userDto);
            Map<String, Object> data = new HashMap<>();
            data.put("username", user.getUsername());
            return ResponseEntity.ok(ApiResponse.success("User registered successfully", data));
        } catch (RuntimeException e) {
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> loginUser(@RequestBody UserDto userDto) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            userDto.username(),
                            userDto.password()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // 获取认证后的用户详细信息
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String jwt = jwtUtil.generateToken(authentication.getName());

            Map<String, Object> data = new HashMap<>();
            data.put("token", jwt);
            data.put("username", userDetails.getUsername()); // 始终返回实际用户名
            data.put("email", userDetails.getEmail()); // 同时返回邮箱

            return ResponseEntity.ok(ApiResponse.success("登录成功", data));
        } catch (Exception e) {
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("error", "Invalid username or password");
            return ResponseEntity.status(401).body(ApiResponse.unauthorized("Invalid username or password"));
        }
    }
}