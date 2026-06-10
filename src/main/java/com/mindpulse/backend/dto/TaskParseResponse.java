package com.mindpulse.backend.dto;

import com.mindpulse.backend.entity.Task;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "AI task parse response")
public class TaskParseResponse {

    @Schema(description = "Raw AI parse result")
    private Map<String, Object> parsedTask;

    @Schema(description = "Created task entity")
    private Task createdTask;

    @Schema(description = "Whether result came from cache")
    private boolean fromCache;

    @Schema(description = "Response time in milliseconds")
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
