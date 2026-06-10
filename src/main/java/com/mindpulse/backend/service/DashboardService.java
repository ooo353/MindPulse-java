package com.mindpulse.backend.service;

import com.mindpulse.backend.dto.CategoryDistributionDto;
import com.mindpulse.backend.dto.DashboardSummaryDto;
import com.mindpulse.backend.dto.ProductivityDto;
import com.mindpulse.backend.dto.StudyHeatmapDto;
import com.mindpulse.backend.mapper.DashboardMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService implements IDashboardService {

    private final DashboardMapper dashboardMapper;

    @Override
    @Cacheable(value = "dashboard", key = "#userId + ':summary'")
    public DashboardSummaryDto getSummary(String userId) {
        log.debug("Computing dashboard summary for user={}", userId);

        int totalTasks = dashboardMapper.countTasksByUser(userId);
        int completedTasks = dashboardMapper.countCompletedTasksByUser(userId);
        double completionRate = totalTasks > 0
                ? Math.round((double) completedTasks / totalTasks * 1000) / 10.0
                : 0.0;
        Double avgHours = dashboardMapper.avgCompletionHoursByUser(userId);
        double avgCompletionHours = avgHours != null ? Math.round(avgHours * 10) / 10.0 : 0.0;

        LocalDateTime since = LocalDateTime.now().minusDays(30);
        int activeDays = dashboardMapper.countActiveDaysByUser(userId, since);
        int totalNotes = dashboardMapper.countNotesByUser(userId);

        return new DashboardSummaryDto(
                totalTasks, completedTasks, completionRate,
                avgCompletionHours, activeDays, totalNotes
        );
    }

    @Override
    @Cacheable(value = "dashboard", key = "#userId + ':productivity:' + #period")
    public ProductivityDto getProductivity(String userId, String period) {
        LocalDateTime since = switch (period != null ? period : "daily") {
            case "weekly" -> LocalDateTime.now().minusWeeks(12);
            case "monthly" -> LocalDateTime.now().minusMonths(12);
            default -> LocalDateTime.now().minusDays(30);
        };

        log.debug("Computing productivity for user={}, period={}, since={}", userId, period, since);

        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> taskRows = dashboardMapper.dailyCompletedTasks(userId, since);
        List<Map<String, Object>> minuteRows = dashboardMapper.dailyStudyMinutes(userId, since, now);

        // Merge into date-indexed maps
        Map<String, Integer> taskMap = new LinkedHashMap<>();
        for (Map<String, Object> row : taskRows) {
            String date = String.valueOf(row.get("date"));
            Object countObj = row.get("count");
            int count = countObj != null ? ((Number) countObj).intValue() : 0;
            taskMap.put(date, count);
        }

        Map<String, Integer> minuteMap = new LinkedHashMap<>();
        for (Map<String, Object> row : minuteRows) {
            String date = String.valueOf(row.get("date"));
            Object minutesObj = row.get("minutes");
            int minutes = minutesObj != null ? ((Number) minutesObj).intValue() : 0;
            minuteMap.put(date, minutes);
        }

        // Build union-sorted date list
        Set<String> allDates = new TreeSet<>(taskMap.keySet());
        allDates.addAll(minuteMap.keySet());

        List<String> dates = new ArrayList<>(allDates);
        List<Integer> completedCounts = new ArrayList<>();
        List<Integer> studyMinutes = new ArrayList<>();
        for (String d : dates) {
            completedCounts.add(taskMap.getOrDefault(d, 0));
            studyMinutes.add(minuteMap.getOrDefault(d, 0));
        }

        return new ProductivityDto(dates, completedCounts, studyMinutes);
    }

    @Override
    @Cacheable(value = "dashboard", key = "#userId + ':categoryDistribution'")
    public CategoryDistributionDto getCategoryDistribution(String userId) {
        log.debug("Computing category distribution for user={}", userId);
        List<Map<String, Object>> taskCategories = dashboardMapper.taskCategoryDistribution(userId);
        List<Map<String, Object>> noteCategories = dashboardMapper.noteCategoryDistribution(userId);
        return new CategoryDistributionDto(taskCategories, noteCategories);
    }

    @Override
    @Cacheable(value = "dashboard", key = "#userId + ':heatmap:' + #year")
    public StudyHeatmapDto getStudyHeatmap(String userId, int year) {
        LocalDateTime start = LocalDateTime.of(year, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(year, 12, 31, 23, 59);

        log.debug("Computing study heatmap for user={}, year={}", userId, year);

        List<Map<String, Object>> rows = dashboardMapper.dailyStudyMinutes(userId, start, end);
        Map<String, Integer> heatmap = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String date = String.valueOf(row.get("date"));
            Object minutesObj = row.get("minutes");
            int minutes = minutesObj != null ? ((Number) minutesObj).intValue() : 0;
            heatmap.put(date, minutes);
        }

        return new StudyHeatmapDto(heatmap);
    }
}
