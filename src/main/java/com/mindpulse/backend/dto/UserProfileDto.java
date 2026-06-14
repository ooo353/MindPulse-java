package com.mindpulse.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "User profile response")
public record UserProfileDto(
    @Schema(description = "User ID", example = "1")
    Long id,

    @Schema(description = "Username", example = "john")
    String username,

    @Schema(description = "Display name", example = "John")
    String nickname,

    @Schema(description = "Email", example = "john@example.com")
    String email,

    @Schema(description = "Avatar URL")
    String avatar,

    @Schema(description = "User role", example = "ROLE_USER")
    String role,

    @Schema(description = "Registration time")
    LocalDateTime createdAt
) {}
