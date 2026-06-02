package com.mindpulse.backend.dto;

import com.mindpulse.backend.entity.Task;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "AI任务解析响应")
public class TaskParseResponse {

    @Schema(description = "AI解析的原始结果")
    private Map<String, Object> parsedTask;

    @Schema(description = "创建的任务实体")
    private Task createdTask;

    @Schema(description = "是否来自缓存")
    private boolean fromCache;

    @Schema(description = "响应时间（毫秒）")
    private long responseTimeMs;

    public TaskParseResponse() {}

    public TaskParseResponse(Map<String, Object> parsedTask, Task createdTask, boolean fromCache, long responseTimeMs) {
        this.parsedTask = parsedTask;
        this.createdTask = createdTask;
        this.fromCache = fromCache;
        this.responseTimeMs = responseTimeMs;
    }

    public Map<String, Object> getParsedTask() {
        return parsedTask;
    }

    public void setParsedTask(Map<String, Object> parsedTask) {
        this.parsedTask = parsedTask;
    }

    public Task getCreatedTask() {
        return createdTask;
    }

    public void setCreatedTask(Task createdTask) {
        this.createdTask = createdTask;
    }

    public boolean isFromCache() {
        return fromCache;
    }

    public void setFromCache(boolean fromCache) {
        this.fromCache = fromCache;
    }

    public long getResponseTimeMs() {
        return responseTimeMs;
    }

    public void setResponseTimeMs(long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }
}
