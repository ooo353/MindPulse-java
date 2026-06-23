package com.mindpulse.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Update user profile request")
public record UpdateProfileDto(
    @Size(min = 1, max = 50, message = "Nickname must be 1-50 characters")
    @Schema(description = "Display name", example = "John")
    String nickname,

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(description = "Email address", example = "john@example.com")
    String email
) {}
