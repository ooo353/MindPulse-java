package com.mindpulse.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "AI task parse request")
public class TaskParseRequest {

    @NotBlank(message = "Task description is required")
    @Schema(description = "Natural language task description", example = "Return books to the library tomorrow at 3pm, high priority, study related", requiredMode = Schema.RequiredMode.REQUIRED)
    private String taskDescription;

    public TaskParseRequest() {}

    public TaskParseRequest(String taskDescription) {
        this.taskDescription = taskDescription;
    }

    public String getTaskDescription() {
        return taskDescription;
    }

    public void setTaskDescription(String taskDescription) {
        this.taskDescription = taskDescription;
    }
}
