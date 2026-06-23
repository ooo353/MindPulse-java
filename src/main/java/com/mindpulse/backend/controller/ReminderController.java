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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/reminders")
@Tag(name = "提醒管理", description = "动态提醒增删改查及智能调度接口，支持多设备同步和分布式并发控制")
@RequiredArgsConstructor
public class ReminderController {

    private final ReminderService reminderService;

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationServiceException("User is not authenticated");
        }
        return authentication.getName();
    }

    @Operation(summary = "创建提醒", description = "创建新的提醒规则，支持一次性/每天/每周/自定义cron类型")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Reminder created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Creation failed")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<Reminder>> createReminder(@Valid @RequestBody ReminderDto dto) {
        String username = getCurrentUsername();
        Reminder created = reminderService.createReminder(dto, username);
        return ResponseEntity.status(201).body(ApiResponse.success(201, "Reminder created successfully", created));
    }

    @Operation(summary = "获取所有提醒", description = "获取当前用户的所有提醒规则")
    @GetMapping
    public ResponseEntity<ApiResponse<List<Reminder>>> getUserReminders() {
        String username = getCurrentUsername();
        List<Reminder> reminders = reminderService.getUserReminders(username);
        return ResponseEntity.ok(ApiResponse.success(reminders));
    }

    @Operation(summary = "获取提醒详情", description = "根据ID获取单个提醒详情")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Reminder>> getReminderById(
            @Parameter(description = "Reminder ID") @PathVariable Long id) {
        String username = getCurrentUsername();
        Reminder reminder = reminderService.getReminderById(id, username);
        return ResponseEntity.ok(ApiResponse.success(reminder));
    }

    @Operation(summary = "更新提醒", description = "修改提醒时间、类型、消息内容等")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Reminder>> updateReminder(
            @Parameter(description = "Reminder ID") @PathVariable Long id,
            @Valid @RequestBody ReminderDto dto) {
        String username = getCurrentUsername();
        Reminder updated = reminderService.updateReminder(id, dto, username);
        return ResponseEntity.ok(ApiResponse.success("Reminder updated successfully", updated));
    }

    @Operation(summary = "删除提醒", description = "根据ID删除提醒规则")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteReminder(
            @Parameter(description = "Reminder ID") @PathVariable Long id) {
        String username = getCurrentUsername();
        reminderService.deleteReminder(id, username);
        return ResponseEntity.ok(ApiResponse.success("Reminder deleted successfully"));
    }
}
