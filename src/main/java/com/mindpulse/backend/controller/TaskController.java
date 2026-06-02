package com.mindpulse.backend.controller;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Tag(name = "任务管理", description = "任务 CRUD 及 AI 解析接口")
public class TaskController {

    private static final Logger log = LoggerFactory.getLogger(TaskController.class);

    @Autowired
    private TaskService taskService;

    @Autowired
    private AiAgentClient aiAgentClient;

    @Autowired
    private SemanticCacheService semanticCacheService;

    @Autowired
    private TaskValidationService taskValidationService;

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : null;
    }

    @Operation(summary = "创建任务", description = "手动创建任务")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "创建成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "服务器错误")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<Task>> createTask(@RequestBody TaskDto taskDto) {
        try {
            String author = getCurrentUsername();
            TaskDto updatedTaskDto = new TaskDto(
                taskDto.id(), taskDto.title(), taskDto.description(), taskDto.dueDate(),
                taskDto.priority(), taskDto.status(), author, taskDto.relatedNotes(), taskDto.category()
            );
            Task createdTask = taskService.createTask(updatedTaskDto);
            return ResponseEntity.status(201).body(ApiResponse.success(201, "Task created successfully", createdTask));
        } catch (Exception e) {
            log.error("创建任务失败: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to create task: " + e.getMessage()));
        }
    }

    @Operation(summary = "获取任务列表", description = "获取当前用户的所有任务，可按状态过滤")
    @GetMapping
    public ResponseEntity<ApiResponse<List<Task>>> getAllTasks(
            @Parameter(description = "状态过滤: pending/completed/archived")
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

    @Operation(summary = "获取任务详情", description = "根据 ID 获取任务详情")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Task>> getTaskById(@PathVariable Long id) {
        try {
            Task task = taskService.getTaskById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
            String username = getCurrentUsername();
            if (!task.getAuthor().equals(username)) {
                return ResponseEntity.status(403).body(ApiResponse.forbidden("Access denied: You don't own this task"));
            }
            return ResponseEntity.ok(ApiResponse.success(task));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "更新任务", description = "根据 ID 更新任务")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Task>> updateTask(@PathVariable Long id, @RequestBody TaskDto taskDto) {
        try {
            Task existingTask = taskService.getTaskById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
            String username = getCurrentUsername();
            if (!existingTask.getAuthor().equals(username)) {
                return ResponseEntity.status(403).body(ApiResponse.forbidden("Access denied: You don't own this task"));
            }
            TaskDto updatedTaskDto = new TaskDto(
                taskDto.id(), taskDto.title(), taskDto.description(), taskDto.dueDate(),
                taskDto.priority(), taskDto.status(), username, taskDto.relatedNotes(), taskDto.category()
            );
            Task updatedTask = taskService.updateTask(id, updatedTaskDto);
            return ResponseEntity.ok(ApiResponse.success("Task updated successfully", updatedTask));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("更新任务失败: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to update task: " + e.getMessage()));
        }
    }

    @Operation(summary = "删除任务", description = "根据 ID 删除任务")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteTask(@PathVariable Long id) {
        try {
            Task existingTask = taskService.getTaskById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
            String username = getCurrentUsername();
            if (!existingTask.getAuthor().equals(username)) {
                return ResponseEntity.status(403).body(ApiResponse.forbidden("Access denied: You don't own this task"));
            }
            taskService.deleteTask(id);
            Map<String, Object> data = new HashMap<>();
            data.put("message", "Task deleted successfully");
            return ResponseEntity.ok(ApiResponse.success("Task deleted successfully", data));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("删除任务失败: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to delete task: " + e.getMessage()));
        }
    }

    @Operation(summary = "AI 任务解析", description = "输入自然语言指令，AI 解析为结构化任务并存入数据库。语义缓存：同义指令直接返回缓存结果。")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "解析成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数错误"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "服务器错误")
    })
    @PostMapping("/parse")
    public ResponseEntity<ApiResponse<TaskParseResponse>> parseTaskWithAI(
            @RequestBody TaskParseRequest request) {

        long startTime = System.currentTimeMillis();
        String taskDescription = request.getTaskDescription();

        if (taskDescription == null || taskDescription.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("任务描述不能为空"));
        }

        log.info("收到 AI 解析请求: {}", taskDescription);

        // 语义归一化 + 哈希
        String normalized = semanticCacheService.normalize(taskDescription);
        String cacheKey = semanticCacheService.hash(normalized);
        log.debug("语义归一化: '{}' -> '{}', hash={}", taskDescription, normalized, cacheKey);

        // 查缓存
        Map<String, Object> parsedTask = semanticCacheService.get(cacheKey, startTime);
        boolean fromCache = parsedTask != null;

        if (!fromCache) {
            // 缓存未命中，调用 AI
            parsedTask = aiAgentClient.parseTask(taskDescription);

            // Java 层校验
            List<String> validationErrors = taskValidationService.validate(parsedTask);
            if (!validationErrors.isEmpty() && parsedTask.get("title") == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.error("AI 解析结果校验失败: " + String.join("; ", validationErrors)));
            }

            // 写入缓存
            semanticCacheService.put(cacheKey, parsedTask, startTime);
        }

        // 创建任务入库
        String author = getCurrentUsername();
        Task createdTask = taskService.createFromParsedData(parsedTask, author);

        long responseTime = System.currentTimeMillis() - startTime;
        TaskParseResponse response = new TaskParseResponse(parsedTask, createdTask, fromCache, responseTime);

        log.info("AI 解析完成: fromCache={}, responseTime={}ms", fromCache, responseTime);
        return ResponseEntity.ok(ApiResponse.success("Task parsed successfully", response));
    }

    @Operation(summary = "缓存统计", description = "获取 AI 解析语义缓存的统计数据，包括命中率、平均响应时间等")
    @GetMapping("/cache-stats")
    public ResponseEntity<ApiResponse<CacheStatsResponse>> getCacheStats() {
        CacheStatsResponse stats = semanticCacheService.getCacheStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @Operation(summary = "更新任务状态（分布式锁保护）", description = "带 Redis 分布式锁的任务状态更新，解决多端并发状态覆盖问题。返回 409 表示锁竞争失败。")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "状态更新成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权操作"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "任务不存在"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "并发冲突，请稍后重试"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "更新失败")
    })
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateTaskStatus(
            @Parameter(description = "任务ID") @PathVariable Long id,
            @Parameter(description = "新状态: pending/completed/archived", required = true)
            @RequestParam String status) {
        try {
            String username = getCurrentUsername();

            // 校验状态值
            if (!List.of("pending", "completed", "archived").contains(status)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.badRequest("无效状态值: " + status + "，仅支持 pending/completed/archived"));
            }

            Task updated = taskService.updateTaskStatusWithLock(id, status, username);
            if (updated == null) {
                Map<String, Object> conflictData = new HashMap<>();
                conflictData.put("message", "并发冲突：任务正在被其他操作修改，请稍后重试");
                conflictData.put("taskId", id);
                return ResponseEntity.status(409).body(ApiResponse.error(409, "并发冲突，请稍后重试"));
            }

            Map<String, Object> data = new HashMap<>();
            data.put("task", updated);
            data.put("message", "任务状态更新成功");
            return ResponseEntity.ok(ApiResponse.success("任务状态更新成功", data));
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            if (e.getMessage() != null && e.getMessage().contains("Access denied")) {
                return ResponseEntity.status(403).body(ApiResponse.forbidden(e.getMessage()));
            }
            log.error("更新任务状态失败: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(ApiResponse.error("更新任务状态失败: " + e.getMessage()));
        }
    }
}
