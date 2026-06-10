package com.mindpulse.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Pomodoro session data transfer object")
public record PomodoroSessionDto(
    @Schema(description = "Session ID", example = "1")
    Long id,

    @Schema(description = "Associated task ID", example = "5")
    Long taskId,

    @Min(value = 1, message = "Duration must be at least 1 minute")
    @Max(value = 180, message = "Duration cannot exceed 180 minutes")
    @Schema(description = "Planned duration in minutes", example = "25")
    Integer durationMinutes,

    @NotBlank(message = "Session type is required")
    @Schema(description = "Session type", example = "focus", allowableValues = {"focus", "short_break", "long_break"})
    String sessionType
) {
}
