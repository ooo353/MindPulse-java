package com.mindpulse.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Admin view of a user")
public record AdminUserDto(
    @Schema(description = "User ID", example = "1")
    Long id,

    @Schema(description = "Username", example = "zhangsan")
    String username,

    @Schema(description = "Email address", example = "zhangsan@example.com")
    String email,

    @Schema(description = "User role", example = "ROLE_USER")
    String role,

    @Schema(description = "Account creation time", example = "2026-06-01T08:00:00")
    LocalDateTime createdAt
) {}
