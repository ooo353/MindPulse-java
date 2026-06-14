package com.mindpulse.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindpulse.backend.dto.CategoryDistributionDto;
import com.mindpulse.backend.dto.DashboardSummaryDto;
import com.mindpulse.backend.dto.ProductivityDto;
import com.mindpulse.backend.dto.StudyHeatmapDto;
import com.mindpulse.backend.mapper.DashboardMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService implements IDashboardService {

    private final DashboardMapper dashboardMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CACHE_PREFIX = "dashboard:";
    private static final long CACHE_TTL_MINUTES = 10;

    // --- Manual cache helpers ---

    private <T> T getFromCache(String key, TypeReference<T> typeRef) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                return objectMapper.readValue(json, typeRef);
            }
        } catch (Exception e) {
            log.warn("Redis cache read failed for key={}: {}", key, e.getMessage());
        }
        return null;
    }

    private void putToCache(String key, Object value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Redis cache write failed for key={}: {}", key, e.getMessage());
        }
    }

    // --- Service methods ---

    @Override
    public DashboardSummaryDto getSummary(String userId) {
        String key = CACHE_PREFIX + userId + ":summary";
        DashboardSummaryDto cached = getFromCache(key, new TypeReference<DashboardSummaryDto>() {});
        if (cached != null) {
            return cached;
        }

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

        DashboardSummaryDto result = new DashboardSummaryDto(
                totalTasks, completedTasks, completionRate,
                avgCompletionHours, activeDays, totalNotes
        );
        putToCache(key, result);
        return result;
    }

    @Override
    public ProductivityDto getProductivity(String userId, String period) {
        String key = CACHE_PREFIX + userId + ":productivity:" + (period != null ? period : "daily");
        ProductivityDto cached = getFromCache(key, new TypeReference<ProductivityDto>() {});
        if (cached != null) {
            return cached;
        }

        LocalDateTime since = switch (period != null ? period : "daily") {
            case "weekly" -> LocalDateTime.now().minusWeeks(12);
            case "monthly" -> LocalDateTime.now().minusMonths(12);
            default -> LocalDateTime.now().minusDays(30);
        };

        log.debug("Computing productivity for user={}, period={}, since={}", userId, period, since);

        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> taskRows = dashboardMapper.dailyCompletedTasks(userId, since);
        List<Map<String, Object>> minuteRows = dashboardMapper.dailyStudyMinutes(userId, since, now);

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

        Set<String> allDates = new TreeSet<>(taskMap.keySet());
        allDates.addAll(minuteMap.keySet());

        List<String> dates = new ArrayList<>(allDates);
        List<Integer> completedCounts = new ArrayList<>();
        List<Integer> studyMinutes = new ArrayList<>();
        for (String d : dates) {
            completedCounts.add(taskMap.getOrDefault(d, 0));
            studyMinutes.add(minuteMap.getOrDefault(d, 0));
        }

        ProductivityDto result = new ProductivityDto(dates, completedCounts, studyMinutes);
        putToCache(key, result);
        return result;
    }

    @Override
    public CategoryDistributionDto getCategoryDistribution(String userId) {
        String key = CACHE_PREFIX + userId + ":categoryDistribution";
        CategoryDistributionDto cached = getFromCache(key, new TypeReference<CategoryDistributionDto>() {});
        if (cached != null) {
            return cached;
        }

        log.debug("Computing category distribution for user={}", userId);
        List<Map<String, Object>> taskCategories = dashboardMapper.taskCategoryDistribution(userId);
        List<Map<String, Object>> noteCategories = dashboardMapper.noteCategoryDistribution(userId);
        CategoryDistributionDto result = new CategoryDistributionDto(taskCategories, noteCategories);
        putToCache(key, result);
        return result;
    }

    @Override
    public StudyHeatmapDto getStudyHeatmap(String userId, int year) {
        String key = CACHE_PREFIX + userId + ":heatmap:" + year;
        StudyHeatmapDto cached = getFromCache(key, new TypeReference<StudyHeatmapDto>() {});
        if (cached != null) {
            return cached;
        }

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

        StudyHeatmapDto result = new StudyHeatmapDto(heatmap);
        putToCache(key, result);
        return result;
    }
}
