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
@Tag(name = "Reminder Management", description = "Dynamic reminder CRUD and smart scheduling interface with multi-device sync and distributed concurrency control")
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

    @Operation(summary = "Create reminder", description = "Create a new reminder rule, supports one-time/daily/weekly/custom cron types")
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

    @Operation(summary = "Get all reminders", description = "Get all reminder rules for the current user")
    @GetMapping
    public ResponseEntity<ApiResponse<List<Reminder>>> getUserReminders() {
        String username = getCurrentUsername();
        List<Reminder> reminders = reminderService.getUserReminders(username);
        return ResponseEntity.ok(ApiResponse.success(reminders));
    }

    @Operation(summary = "Get reminder by ID", description = "Get single reminder details by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Reminder>> getReminderById(
            @Parameter(description = "Reminder ID") @PathVariable Long id) {
        String username = getCurrentUsername();
        Reminder reminder = reminderService.getReminderById(id, username);
        return ResponseEntity.ok(ApiResponse.success(reminder));
    }

    @Operation(summary = "Update reminder", description = "Modify reminder time, type, message content, etc.")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Reminder>> updateReminder(
            @Parameter(description = "Reminder ID") @PathVariable Long id,
            @Valid @RequestBody ReminderDto dto) {
        String username = getCurrentUsername();
        Reminder updated = reminderService.updateReminder(id, dto, username);
        return ResponseEntity.ok(ApiResponse.success("Reminder updated successfully", updated));
    }

    @Operation(summary = "Delete reminder", description = "Delete reminder rule by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteReminder(
            @Parameter(description = "Reminder ID") @PathVariable Long id) {
        String username = getCurrentUsername();
        reminderService.deleteReminder(id, username);
        return ResponseEntity.ok(ApiResponse.success("Reminder deleted successfully"));
    }
}
