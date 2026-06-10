package com.mindpulse.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Productivity trend data")
public record ProductivityDto(
    @Schema(description = "Date labels")
    List<String> dates,

    @Schema(description = "Completed task counts per date")
    List<Integer> completedCounts,

    @Schema(description = "Study minutes per date")
    List<Integer> studyMinutes
) {}
