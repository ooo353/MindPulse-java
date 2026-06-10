package com.mindpulse.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Audit log entry")
public record AuditLogDto(
    @Schema(description = "Audit log ID", example = "1")
    Long id,

    @Schema(description = "User who performed the action", example = "zhangsan")
    String userId,

    @Schema(description = "Action performed", example = "CREATE")
    String action,

    @Schema(description = "Resource type affected", example = "TASK")
    String resourceType,

    @Schema(description = "Resource ID affected", example = "42")
    Long resourceId,

    @Schema(description = "Client IP address", example = "127.0.0.1")
    String ipAddress,

    @Schema(description = "Additional details about the action", example = "Created task: Buy groceries")
    String details,

    @Schema(description = "When the action occurred", example = "2026-06-08T10:30:00")
    LocalDateTime createdAt
) {}
