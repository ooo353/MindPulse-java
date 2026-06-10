package com.mindpulse.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.time.LocalTime;

@Schema(description = "Reminder request body")
public record ReminderDto(
        @Schema(description = "Reminder ID (not required for creation)") Long id,

        @NotBlank(message = "Reminder message is required")
        @Schema(description = "Reminder content", example = "Review chapter 3 of calculus") String message,

        @Pattern(regexp = "ONCE|DAILY|WEEKLY|CUSTOM", message = "Remind type must be ONCE, DAILY, WEEKLY, or CUSTOM")
        @Schema(description = "Reminder type: ONCE/DAILY/WEEKLY/CUSTOM", example = "DAILY") String remindType,

        @Schema(description = "Reminder time (HH:mm)", example = "09:00") LocalTime remindTime,
        @Schema(description = "One-time reminder date (yyyy-MM-dd)") LocalDate remindDate,
        @Schema(description = "Weekly reminder day of week (MON/TUE/WED/...)") String dayOfWeek,
        @Schema(description = "Custom cron expression") String cronExpression,
        @Schema(description = "Associated target ID") Long targetId,
        @Schema(description = "Associated target type: TASK/NOTE") String targetType,
        @Schema(description = "Whether enabled", example = "true") Boolean enabled
) {
    public ReminderDto {
        if (remindType == null || remindType.isBlank()) {
            remindType = "ONCE";
        }
        if (enabled == null) {
            enabled = true;
        }
    }
}
