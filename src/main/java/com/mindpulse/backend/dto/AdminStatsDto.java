package com.mindpulse.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Admin dashboard statistics")
public record AdminStatsDto(
    @Schema(description = "Total registered users", example = "150")
    int totalUsers,

    @Schema(description = "Total audit log entries", example = "5000")
    int totalAuditLogs,

    @Schema(description = "Today's actions count", example = "42")
    int todayActions
) {
}
