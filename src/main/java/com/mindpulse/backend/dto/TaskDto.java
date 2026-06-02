package com.mindpulse.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "任务数据传输对象")
public record TaskDto(
    @Schema(description = "任务ID", example = "1")
    Long id,

    @Schema(description = "任务标题", example = "去图书馆还书")
    String title,

    @Schema(description = "任务描述", example = "去图书馆还书")
    String description,

    @Schema(description = "截止时间", example = "2026-05-24T15:00:00")
    LocalDateTime dueDate,

    @Schema(description = "优先级", example = "high", allowableValues = {"high", "medium", "low"})
    String priority,

    @Schema(description = "状态", example = "pending", allowableValues = {"pending", "completed", "archived"})
    String status,

    @Schema(description = "创建者用户名", example = "zhangsan")
    String author,

    @Schema(description = "关联笔记ID，逗号分隔", example = "1,3")
    String relatedNotes,

    @Schema(description = "分类标签，逗号分隔", example = "学习,紧急")
    String category
) {
    public TaskDto(String title, String description, String author) {
        this(null, title, description, null, "medium", "pending", author, null, null);
    }
}
