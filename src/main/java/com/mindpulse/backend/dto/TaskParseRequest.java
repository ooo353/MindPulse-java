package com.mindpulse.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "AI任务解析请求")
public class TaskParseRequest {

    @NotBlank(message = "任务描述不能为空")
    @Schema(description = "自然语言任务描述", example = "明天下午3点去图书馆还书，优先级高，学习类", requiredMode = Schema.RequiredMode.REQUIRED)
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
