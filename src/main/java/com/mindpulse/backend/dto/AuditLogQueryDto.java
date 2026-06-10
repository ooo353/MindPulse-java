package com.mindpulse.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Audit log query parameters")
public record AuditLogQueryDto(
    @Schema(description = "Filter by action type", example = "CREATE")
    String action,

    @Schema(description = "Filter by resource type", example = "TASK")
    String resourceType,

    @Schema(description = "Filter by user ID", example = "zhangsan")
    String userId,

    @Schema(description = "Page number (1-based)", example = "1")
    int page,

    @Schema(description = "Page size", example = "20")
    int size
) {
    public AuditLogQueryDto {
        if (page < 1) page = 1;
        if (size < 1) size = 20;
        if (size > 100) size = 100;
    }

    public AuditLogQueryDto() {
        this(null, null, null, 1, 20);
    }
}
