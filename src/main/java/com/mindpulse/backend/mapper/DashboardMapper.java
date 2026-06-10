package com.mindpulse.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface DashboardMapper {
    int countTasksByUser(@Param("userId") String userId);
    int countCompletedTasksByUser(@Param("userId") String userId);
    Double avgCompletionHoursByUser(@Param("userId") String userId);
    int countActiveDaysByUser(@Param("userId") String userId, @Param("since") LocalDateTime since);
    int countNotesByUser(@Param("userId") String userId);
    List<Map<String, Object>> taskCategoryDistribution(@Param("userId") String userId);
    List<Map<String, Object>> noteCategoryDistribution(@Param("userId") String userId);
    List<Map<String, Object>> dailyCompletedTasks(@Param("userId") String userId, @Param("since") LocalDateTime since);
    List<Map<String, Object>> dailyStudyMinutes(@Param("userId") String userId, @Param("since") LocalDateTime since, @Param("end") LocalDateTime end);
}
