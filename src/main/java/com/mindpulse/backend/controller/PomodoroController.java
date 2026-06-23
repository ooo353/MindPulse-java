package com.mindpulse.backend.controller;

import com.mindpulse.backend.dto.ApiResponse;
import com.mindpulse.backend.dto.PomodoroSessionDto;
import com.mindpulse.backend.dto.PomodoroStatsDto;
import com.mindpulse.backend.entity.PomodoroSession;
import com.mindpulse.backend.service.IPomodoroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/pomodoro")
@Tag(name = "番茄钟", description = "番茄钟计时器和学习统计接口")
@RequiredArgsConstructor
public class PomodoroController {

    private final IPomodoroService pomodoroService;

    @Operation(summary = "开始番茄钟", description = "开始一个新的专注/休息会话")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Session started"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/start")
    public ResponseEntity<ApiResponse<PomodoroSession>> startSession(
            @Valid @RequestBody PomodoroSessionDto dto) {
        String userId = getCurrentUsername();
        PomodoroSession session = pomodoroService.startSession(dto, userId);
        return ResponseEntity.status(201).body(ApiResponse.success(201, "Pomodoro session started", session));
    }

    @Operation(summary = "完成番茄钟", description = "将运行中的会话标记为已完成")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Session completed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not your session"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Session not found")
    })
    @PutMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<PomodoroSession>> completeSession(
            @Parameter(description = "Session ID") @PathVariable Long id) {
        String userId = getCurrentUsername();
        PomodoroSession session = pomodoroService.completeSession(id, userId);
        return ResponseEntity.ok(ApiResponse.success(200, "Pomodoro session completed", session));
    }

    @Operation(summary = "取消番茄钟", description = "取消一个运行中的会话")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Session cancelled"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not your session"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Session not found")
    })
    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<PomodoroSession>> cancelSession(
            @Parameter(description = "Session ID") @PathVariable Long id) {
        String userId = getCurrentUsername();
        PomodoroSession session = pomodoroService.cancelSession(id, userId);
        return ResponseEntity.ok(ApiResponse.success(200, "Pomodoro session cancelled", session));
    }

    @Operation(summary = "获取当前会话", description = "获取当前正在运行的番茄钟会话")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Active session or null")
    })
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<PomodoroSession>> getActiveSession() {
        String userId = getCurrentUsername();
        Optional<PomodoroSession> session = pomodoroService.getActiveSession(userId);
        return ResponseEntity.ok(ApiResponse.success(200, "Active session retrieved", session.orElse(null)));
    }

    @Operation(summary = "获取学习统计", description = "获取指定时间段的番茄钟统计数据")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Statistics retrieved")
    })
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<PomodoroStatsDto>> getStats(
            @Parameter(description = "Period: daily, weekly, monthly")
            @RequestParam(defaultValue = "daily") String period) {
        String userId = getCurrentUsername();
        PomodoroStatsDto stats = pomodoroService.getStats(userId, period);
        return ResponseEntity.ok(ApiResponse.success(200, "Statistics retrieved", stats));
    }

    @Operation(summary = "获取会话历史", description = "获取分页的番茄钟会话历史记录")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "History retrieved")
    })
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<PomodoroSession>>> getHistory(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        if (size > 100) size = 100;
        String userId = getCurrentUsername();
        List<PomodoroSession> history = pomodoroService.getHistory(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(200, "History retrieved", history));
    }

    @Operation(summary = "删除番茄钟会话")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSession(
            @Parameter(description = "Session ID") @PathVariable Long id) {
        String userId = getCurrentUsername();
        pomodoroService.deleteSession(id, userId);
        return ResponseEntity.ok(ApiResponse.success(200, "Session deleted", null));
    }

    @Operation(summary = "清除所有番茄钟历史")
    @DeleteMapping("/history")
    public ResponseEntity<ApiResponse<Void>> clearHistory() {
        String userId = getCurrentUsername();
        pomodoroService.clearHistory(userId);
        return ResponseEntity.ok(ApiResponse.success(200, "History cleared", null));
    }

    @Operation(summary = "获取每日摘要（饼图）")
    @GetMapping("/daily-summary")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getDailySummary(
            @RequestParam(defaultValue = "") String date) {
        String userId = getCurrentUsername();
        if (date.isEmpty()) date = LocalDate.now().toString();
        List<Map<String, Object>> summary = pomodoroService.getDailySummary(userId, date);
        return ResponseEntity.ok(ApiResponse.success(200, "Daily summary retrieved", summary));
    }

    private String getCurrentUsername() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationServiceException("User is not authenticated");
        }
        return authentication.getName();
    }
}
