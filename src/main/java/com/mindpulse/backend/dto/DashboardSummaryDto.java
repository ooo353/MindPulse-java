package com.mindpulse.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;

@Schema(description = "Dashboard summary statistics")
public record DashboardSummaryDto(
    @Schema(description = "Total tasks", example = "50")
    int totalTasks,

    @Schema(description = "Completed tasks", example = "35")
    int completedTasks,

    @Schema(description = "Completion rate percentage", example = "70.0")
    double completionRate,

    @Schema(description = "Average completion time in hours", example = "2.5")
    double avgCompletionHours,

    @Schema(description = "Active study days", example = "15")
    int activeDays,

    @Schema(description = "Total notes", example = "20")
    int totalNotes
) implements Serializable {}
