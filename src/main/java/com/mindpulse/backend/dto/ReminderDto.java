package com.mindpulse.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;

@Schema(description = "提醒请求体")
public record ReminderDto(
        @Schema(description = "提醒ID（创建时无需传）") Long id,
        @Schema(description = "提醒内容", example = "记得复习高数第三章") String message,
        @Schema(description = "提醒类型: ONCE/DAILY/WEEKLY/CUSTOM", example = "DAILY") String remindType,
        @Schema(description = "提醒时间 (HH:mm)", example = "09:00") LocalTime remindTime,
        @Schema(description = "一次性提醒日期 (yyyy-MM-dd)") LocalDate remindDate,
        @Schema(description = "每周提醒星期几 (MON/TUE/WED/...)") String dayOfWeek,
        @Schema(description = "自定义 cron 表达式") String cronExpression,
        @Schema(description = "关联目标ID") Long targetId,
        @Schema(description = "关联目标类型: TASK/NOTE") String targetType,
        @Schema(description = "是否启用", example = "true") Boolean enabled
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
