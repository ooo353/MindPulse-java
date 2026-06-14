package com.mindpulse.backend.mapper;

import com.mindpulse.backend.entity.PomodoroSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface PomodoroSessionMapper {

    PomodoroSession findById(@Param("id") Long id);

    List<PomodoroSession> findByUserId(@Param("userId") String userId);

    List<PomodoroSession> findByUserIdAndStatus(
        @Param("userId") String userId,
        @Param("status") String status
    );

    void insert(PomodoroSession session);

    void updateStatus(
        @Param("id") Long id,
        @Param("status") String status,
        @Param("endTime") LocalDateTime endTime,
        @Param("actualMinutes") Integer actualMinutes
    );

    int countByUserIdAndDateRange(
        @Param("userId") String userId,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

    Integer sumMinutesByUserIdAndDateRange(
        @Param("userId") String userId,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

    int countByUserIdAndStatusAndDateRange(
        @Param("userId") String userId,
        @Param("status") String status,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

    List<PomodoroSession> findRecentByUserId(
        @Param("userId") String userId,
        @Param("limit") int limit
    );

    List<PomodoroSession> findByUserIdPaged(
        @Param("userId") String userId,
        @Param("offset") int offset,
        @Param("limit") int limit
    );

    List<String> findDistinctCompletedDates(@Param("userId") String userId);

    void deleteById(@Param("id") Long id);

    void deleteByUserId(@Param("userId") String userId);

    List<Map<String, Object>> dailySummary(@Param("userId") String userId, @Param("date") String date);
}
