package com.mindpulse.backend.controller;

import com.mindpulse.backend.annotation.AuditLogAnnotation;
import com.mindpulse.backend.dto.ApiResponse;
import com.mindpulse.backend.dto.CacheStatsResponse;
import com.mindpulse.backend.dto.TaskDto;
import com.mindpulse.backend.dto.TaskParseRequest;
import com.mindpulse.backend.dto.TaskParseResponse;
import com.mindpulse.backend.entity.Task;
import com.mindpulse.backend.exception.ResourceNotFoundException;
import com.mindpulse.backend.service.AiAgentClient;
import com.mindpulse.backend.service.TaskService;
import com.mindpulse.backend.service.ai.SemanticCacheService;
import com.mindpulse.backend.service.ai.TaskValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/tasks")
@Tag(name = "Task Management", description = "Task CRUD and AI parsing interface")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final AiAgentClient aiAgentClient;
    private final SemanticCacheService semanticCacheService;
    private final TaskValidationService taskValidationService;

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationServiceException("User is not authenticated");
        }
        return authentication.getName();
    }

    @Operation(summary = "Create task", description = "Manually create a task")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Server error")
    })
    @AuditLogAnnotation(action = "CREATE", resourceType = "TASK")
    @PostMapping
    public ResponseEntity<ApiResponse<Task>> createTask(@Valid @RequestBody TaskDto taskDto) {
        String author = getCurrentUsername();
        TaskDto updatedTaskDto = new TaskDto(
            taskDto.id(), taskDto.title(), taskDto.description(), taskDto.dueDate(),
            taskDto.priority(), taskDto.status(), author, taskDto.relatedNotes(), taskDto.category()
        );
        Task createdTask = taskService.createTask(updatedTaskDto);
        return ResponseEntity.status(201).body(ApiResponse.success(201, "Task created successfully", createdTask));
    }

    @Operation(summary = "Get all tasks", description = "Get all tasks for the current user, optionally filtered by status")
    @GetMapping
    public ResponseEntity<ApiResponse<List<Task>>> getAllTasks(
            @Parameter(description = "Status filter: pending/completed/archived")
            @RequestParam(required = false) String status) {
        String username = getCurrentUsername();
        List<Task> tasks;
        if (status != null && !status.isEmpty()) {
            tasks = taskService.getTasksByUserAndStatus(username, status);
        } else {
            tasks = taskService.getAllTasksByUser(username);
        }
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    @Operation(summary = "Get task by ID", description = "Get task details by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Task>> getTaskById(@PathVariable Long id) {
        Task task = taskService.getTaskById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
        String username = getCurrentUsername();
        taskService.verifyOwnership(task.getAuthor(), username, id);
        return ResponseEntity.ok(ApiResponse.success(task));
    }

    @Operation(summary = "Update task", description = "Update a task by ID")
    @AuditLogAnnotation(action = "UPDATE", resourceType = "TASK")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Task>> updateTask(@PathVariable Long id, @Valid @RequestBody TaskDto taskDto) {
        Task existingTask = taskService.getTaskById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
        String username = getCurrentUsername();
        taskService.verifyOwnership(existingTask.getAuthor(), username, id);
        TaskDto updatedTaskDto = new TaskDto(
            taskDto.id(), taskDto.title(), taskDto.description(), taskDto.dueDate(),
            taskDto.priority(), taskDto.status(), username, taskDto.relatedNotes(), taskDto.category()
        );
        Task updatedTask = taskService.updateTask(id, updatedTaskDto);
        return ResponseEntity.ok(ApiResponse.success("Task updated successfully", updatedTask));
    }

    @Operation(summary = "Delete task", description = "Delete a task by ID")
    @AuditLogAnnotation(action = "DELETE", resourceType = "TASK")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteTask(@PathVariable Long id) {
        Task existingTask = taskService.getTaskById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
        String username = getCurrentUsername();
        taskService.verifyOwnership(existingTask.getAuthor(), username, id);
        taskService.deleteTask(id);
        Map<String, Object> data = new HashMap<>();
        data.put("message", "Task deleted successfully");
        return ResponseEntity.ok(ApiResponse.success("Task deleted successfully", data));
    }

    @Operation(summary = "AI task parsing", description = "Parse natural language instruction into a structured task via AI. Semantic cache: synonymous instructions return cached results.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Parse successful"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid parameters"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Server error")
    })
    @PostMapping("/parse")
    public ResponseEntity<ApiResponse<TaskParseResponse>> parseTaskWithAI(
            @Valid @RequestBody TaskParseRequest request) {

        long startTime = System.currentTimeMillis();
        String taskDescription = request.getTaskDescription();

        if (taskDescription == null || taskDescription.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("Task description is required"));
        }

        log.info("Received AI parse request: {}", taskDescription);

        String normalized = semanticCacheService.normalize(taskDescription);
        String cacheKey = semanticCacheService.hash(normalized);
        log.debug("Semantic normalization: '{}' -> '{}', hash={}", taskDescription, normalized, cacheKey);

        Map<String, Object> parsedTask = semanticCacheService.get(cacheKey, startTime);
        boolean fromCache = parsedTask != null;

        if (!fromCache) {
            parsedTask = aiAgentClient.parseTask(taskDescription);

            List<String> validationErrors = taskValidationService.validate(parsedTask);
            String title = (String) parsedTask.get("title");
            if (!validationErrors.isEmpty() && (title == null || title.isBlank())) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.error("AI parse validation failed: " + String.join("; ", validationErrors)));
            }

            semanticCacheService.put(cacheKey, parsedTask, startTime);
        }

        String author = getCurrentUsername();
        Task createdTask = taskService.createFromParsedData(parsedTask, author);

        long responseTime = System.currentTimeMillis() - startTime;
        TaskParseResponse response = new TaskParseResponse(parsedTask, createdTask, fromCache, responseTime);

        log.info("AI parse completed: fromCache={}, responseTime={}ms", fromCache, responseTime);
        return ResponseEntity.ok(ApiResponse.success("Task parsed successfully", response));
    }

    @Operation(summary = "Cache statistics", description = "Get AI parse semantic cache statistics including hit rate and average response time")
    @GetMapping("/cache-stats")
    public ResponseEntity<ApiResponse<CacheStatsResponse>> getCacheStats() {
        CacheStatsResponse stats = semanticCacheService.getCacheStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @Operation(summary = "Update task status (distributed lock)", description = "Task status update with Redis distributed lock to prevent multi-device concurrent status conflicts. Returns 409 on lock contention.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Status updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Task not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Concurrency conflict, please retry"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Update failed")
    })
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateTaskStatus(
            @Parameter(description = "Task ID") @PathVariable Long id,
            @Parameter(description = "New status: pending/completed/archived", required = true)
            @RequestParam String status) {
        String username = getCurrentUsername();

        if (!List.of("pending", "completed", "archived").contains(status)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("Invalid status value: " + status + ". Only pending/completed/archived are supported"));
        }

        Task updated = taskService.updateTaskStatusWithLock(id, status, username);
        if (updated == null) {
            Map<String, Object> conflictData = new HashMap<>();
            conflictData.put("message", "Concurrency conflict: the task is being modified by another operation, please retry");
            conflictData.put("taskId", id);
            return ResponseEntity.status(409).body(ApiResponse.error(409, "Concurrency conflict, please retry"));
        }

        Map<String, Object> data = new HashMap<>();
        data.put("task", updated);
        data.put("message", "Task status updated successfully");
        return ResponseEntity.ok(ApiResponse.success("Task status updated successfully", data));
    }
}
