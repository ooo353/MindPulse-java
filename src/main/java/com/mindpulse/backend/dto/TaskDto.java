package com.mindpulse.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

@Schema(description = "Task data transfer object")
public record TaskDto(
    @Schema(description = "Task ID", example = "1")
    Long id,

    @NotBlank(message = "Task title is required")
    @Schema(description = "Task title", example = "Return books to the library")
    String title,

    @Schema(description = "Task description", example = "Return books to the library")
    String description,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Due date", example = "2026-05-24T15:00:00")
    LocalDateTime dueDate,

    @Schema(description = "Priority", example = "high", allowableValues = {"high", "medium", "low"})
    String priority,

    @Schema(description = "Status", example = "pending", allowableValues = {"pending", "completed", "archived"})
    String status,

    @Schema(description = "Creator username", example = "zhangsan")
    String author,

    @Schema(description = "Related note IDs, comma-separated", example = "1,3")
    String relatedNotes,

    @Schema(description = "Category tags, comma-separated", example = "study,urgent")
    String category
) {
    public TaskDto(String title, String description, String author) {
        this(null, title, description, null, "medium", "pending", author, null, null);
    }
}
