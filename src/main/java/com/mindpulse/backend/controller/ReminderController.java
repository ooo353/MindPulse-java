package com.mindpulse.backend.controller;

import com.mindpulse.backend.dto.ApiResponse;
import com.mindpulse.backend.dto.ReminderDto;
import com.mindpulse.backend.entity.Reminder;
import com.mindpulse.backend.exception.ResourceNotFoundException;
import com.mindpulse.backend.service.ReminderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reminders")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Tag(name = "智能提醒管理", description = "动态提醒 CRUD 及智能调度接口，支持多端同步与分布式并发控制")
public class ReminderController {

    private static final Logger log = LoggerFactory.getLogger(ReminderController.class);

    @Autowired
    private ReminderService reminderService;

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : null;
    }

    @Operation(summary = "创建提醒", description = "创建一个新的提醒规则，支持一次性/每日/每周/自定义 cron 类型")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "提醒创建成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "创建失败")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<Reminder>> createReminder(@RequestBody ReminderDto dto) {
        try {
            String username = getCurrentUsername();
            Reminder created = reminderService.createReminder(dto, username);
            return ResponseEntity.status(201).body(ApiResponse.success(201, "提醒创建成功", created));
        } catch (Exception e) {
            log.error("创建提醒失败: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(ApiResponse.error("创建提醒失败: " + e.getMessage()));
        }
    }

    @Operation(summary = "获取所有提醒", description = "获取当前用户的所有提醒规则列表")
    @GetMapping
    public ResponseEntity<ApiResponse<List<Reminder>>> getUserReminders() {
        String username = getCurrentUsername();
        List<Reminder> reminders = reminderService.getUserReminders(username);
        return ResponseEntity.ok(ApiResponse.success(reminders));
    }

    @Operation(summary = "获取提醒详情", description = "根据ID获取单条提醒详情")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Reminder>> getReminderById(
            @Parameter(description = "提醒ID") @PathVariable Long id) {
        try {
            String username = getCurrentUsername();
            Reminder reminder = reminderService.getReminderById(id, username);
            return ResponseEntity.ok(ApiResponse.success(reminder));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "更新提醒", description = "修改提醒规则的时间、类型、消息内容等")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Reminder>> updateReminder(
            @Parameter(description = "提醒ID") @PathVariable Long id,
            @RequestBody ReminderDto dto) {
        try {
            String username = getCurrentUsername();
            Reminder updated = reminderService.updateReminder(id, dto, username);
            return ResponseEntity.ok(ApiResponse.success("提醒更新成功", updated));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("更新提醒失败: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(ApiResponse.error("更新提醒失败: " + e.getMessage()));
        }
    }

    @Operation(summary = "删除提醒", description = "根据ID删除提醒规则")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteReminder(
            @Parameter(description = "提醒ID") @PathVariable Long id) {
        try {
            String username = getCurrentUsername();
            reminderService.deleteReminder(id, username);
            return ResponseEntity.ok(ApiResponse.success("提醒已删除"));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("删除提醒失败: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(ApiResponse.error("删除提醒失败: " + e.getMessage()));
        }
    }
}
