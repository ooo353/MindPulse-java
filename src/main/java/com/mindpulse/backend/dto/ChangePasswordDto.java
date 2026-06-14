package com.mindpulse.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Change password request")
public record ChangePasswordDto(
    @NotBlank(message = "Current password is required")
    @Schema(description = "Current password")
    String oldPassword,

    @NotBlank(message = "New password is required")
    @Size(min = 6, max = 100, message = "New password must be 6-100 characters")
    @Schema(description = "New password")
    String newPassword
) {}
