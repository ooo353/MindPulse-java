package com.mindpulse.backend.controller;

import com.mindpulse.backend.dto.ApiResponse;
import com.mindpulse.backend.dto.CategoryDistributionDto;
import com.mindpulse.backend.dto.DashboardSummaryDto;
import com.mindpulse.backend.dto.ProductivityDto;
import com.mindpulse.backend.dto.StudyHeatmapDto;
import com.mindpulse.backend.service.IDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Year;

@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "仪表盘", description = "仪表盘统计与数据分析接口")
@RequiredArgsConstructor
public class DashboardController {

    private final IDashboardService dashboardService;

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationServiceException("User is not authenticated");
        }
        return authentication.getName();
    }

    @Operation(summary = "获取仪表盘摘要", description = "获取任务总数、完成率、活跃天数等统计信息")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Summary retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Server error")
    })
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardSummaryDto>> getSummary() {
        String username = getCurrentUsername();
        DashboardSummaryDto summary = dashboardService.getSummary(username);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @Operation(summary = "获取生产力数据", description = "获取指定时间段内每日完成任务数和学习时长")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Productivity data retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Server error")
    })
    @GetMapping("/productivity")
    public ResponseEntity<ApiResponse<ProductivityDto>> getProductivity(
            @Parameter(description = "Period: daily (30 days), weekly (12 weeks), monthly (12 months)")
            @RequestParam(defaultValue = "daily") String period) {
        String username = getCurrentUsername();
        ProductivityDto productivity = dashboardService.getProductivity(username, period);
        return ResponseEntity.ok(ApiResponse.success(productivity));
    }

    @Operation(summary = "获取分类分布", description = "获取任务和笔记的分类分布")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Category distribution retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Server error")
    })
    @GetMapping("/category-distribution")
    public ResponseEntity<ApiResponse<CategoryDistributionDto>> getCategoryDistribution() {
        String username = getCurrentUsername();
        CategoryDistributionDto distribution = dashboardService.getCategoryDistribution(username);
        return ResponseEntity.ok(ApiResponse.success(distribution));
    }

    @Operation(summary = "获取学习热力图", description = "获取指定年份每日学习时长热力图")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Heatmap retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Server error")
    })
    @GetMapping("/study-heatmap")
    public ResponseEntity<ApiResponse<StudyHeatmapDto>> getStudyHeatmap(
            @Parameter(description = "Year (e.g. 2026)")
            @RequestParam(defaultValue = "2026") int year) {
        String username = getCurrentUsername();
        StudyHeatmapDto heatmap = dashboardService.getStudyHeatmap(username, year);
        return ResponseEntity.ok(ApiResponse.success(heatmap));
    }
}
