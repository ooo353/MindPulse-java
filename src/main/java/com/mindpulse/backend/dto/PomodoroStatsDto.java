package com.mindpulse.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Pomodoro study statistics")
public record PomodoroStatsDto(
    @Schema(description = "Total pomodoro sessions", example = "42")
    int totalSessions,

    @Schema(description = "Completed pomodoro sessions", example = "38")
    int completedSessions,

    @Schema(description = "Total focus minutes", example = "950")
    int totalMinutes,

    @Schema(description = "Today's pomodoro sessions", example = "4")
    int todaySessions,

    @Schema(description = "Today's focus minutes", example = "100")
    int todayMinutes,

    @Schema(description = "Consecutive days with at least one completed session", example = "7")
    int streakDays
) {
}
