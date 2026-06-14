package com.mindpulse.backend.service;

import com.mindpulse.backend.dto.PomodoroSessionDto;
import com.mindpulse.backend.dto.PomodoroStatsDto;
import com.mindpulse.backend.entity.PomodoroSession;
import com.mindpulse.backend.exception.ResourceNotFoundException;
import com.mindpulse.backend.mapper.PomodoroSessionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PomodoroService implements IPomodoroService {

    private final PomodoroSessionMapper pomodoroSessionMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String ACTIVE_SESSION_KEY = "pomodoro:active:%s";
    private static final long ACTIVE_SESSION_TTL_MINUTES = 30;

    private void evictDashboardCache() {
        try {
            var keys = redisTemplate.keys("dashboard:*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("Redis dashboard cache evict failed: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public PomodoroSession startSession(PomodoroSessionDto dto, String userId) {
        // Prevent duplicate active sessions
        Optional<PomodoroSession> existing = getActiveSession(userId);
        if (existing.isPresent()) {
            throw new IllegalStateException("User already has an active pomodoro session (id: " + existing.get().getId() + ")");
        }

        int duration = dto.durationMinutes() != null ? dto.durationMinutes() : 25;
        PomodoroSession session = new PomodoroSession(userId, dto.taskId(), duration, dto.sessionType());
        session.setTaskDescription(dto.taskDescription());
        session.setStartTime(LocalDateTime.now());

        pomodoroSessionMapper.insert(session);
        log.info("Pomodoro session {} started for user {}", session.getId(), userId);

        String redisKey = String.format(ACTIVE_SESSION_KEY, userId);
        redisTemplate.opsForValue().set(redisKey, session, ACTIVE_SESSION_TTL_MINUTES, TimeUnit.MINUTES);

        evictDashboardCache();
        return session;
    }

    @Override
    @Transactional
    public PomodoroSession completeSession(Long id, String userId) {
        PomodoroSession session = pomodoroSessionMapper.findById(id);
        if (session == null) {
            throw new ResourceNotFoundException("Pomodoro session not found with id: " + id);
        }
        verifyOwnership(session.getUserId(), userId, id);

        if (!"running".equals(session.getStatus())) {
            throw new IllegalStateException("Session " + id + " is not running (current status: " + session.getStatus() + ")");
        }

        LocalDateTime now = LocalDateTime.now();
        int actualMinutes = (int) Duration.between(session.getStartTime(), now).toMinutes();
        pomodoroSessionMapper.updateStatus(id, "completed", now, actualMinutes);
        log.info("Pomodoro session {} completed by user {} with {} minutes", id, userId, actualMinutes);

        String redisKey = String.format(ACTIVE_SESSION_KEY, userId);
        redisTemplate.delete(redisKey);

        session.setStatus("completed");
        session.setEndTime(now);
        session.setActualMinutes(actualMinutes);
        evictDashboardCache();
        return session;
    }

    @Override
    @Transactional
    public PomodoroSession cancelSession(Long id, String userId) {
        PomodoroSession session = pomodoroSessionMapper.findById(id);
        if (session == null) {
            throw new ResourceNotFoundException("Pomodoro session not found with id: " + id);
        }
        verifyOwnership(session.getUserId(), userId, id);

        if (!"running".equals(session.getStatus())) {
            throw new IllegalStateException("Session " + id + " is not running (current status: " + session.getStatus() + ")");
        }

        pomodoroSessionMapper.updateStatus(id, "cancelled", LocalDateTime.now(), null);
        log.info("Pomodoro session {} cancelled by user {}", id, userId);

        String redisKey = String.format(ACTIVE_SESSION_KEY, userId);
        redisTemplate.delete(redisKey);

        session.setStatus("cancelled");
        session.setEndTime(LocalDateTime.now());
        evictDashboardCache();
        return session;
    }

    @Override
    public Optional<PomodoroSession> getActiveSession(String userId) {
        String redisKey = String.format(ACTIVE_SESSION_KEY, userId);
        Object cached = redisTemplate.opsForValue().get(redisKey);
        if (cached instanceof PomodoroSession session && "running".equals(session.getStatus())) {
            return Optional.of(session);
        }

        List<PomodoroSession> running = pomodoroSessionMapper.findByUserIdAndStatus(userId, "running");
        if (running.isEmpty()) {
            return Optional.empty();
        }
        PomodoroSession session = running.get(0);
        redisTemplate.opsForValue().set(
            String.format(ACTIVE_SESSION_KEY, userId),
            session,
            ACTIVE_SESSION_TTL_MINUTES,
            TimeUnit.MINUTES
        );
        return Optional.of(session);
    }

    @Override
    public PomodoroStatsDto getStats(String userId, String period) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start;

        switch (period != null ? period : "daily") {
            case "weekly" -> start = now.minusWeeks(1);
            case "monthly" -> start = now.minusMonths(1);
            default -> start = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        }

        int totalSessions = pomodoroSessionMapper.countByUserIdAndDateRange(userId, start, now);
        int completedSessions = pomodoroSessionMapper.countByUserIdAndStatusAndDateRange(userId, "completed", start, now);
        Integer totalMinutesObj = pomodoroSessionMapper.sumMinutesByUserIdAndDateRange(userId, start, now);
        int totalMinutes = totalMinutesObj != null ? totalMinutesObj : 0;

        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        int todaySessions = pomodoroSessionMapper.countByUserIdAndDateRange(userId, todayStart, now);
        Integer todayMinutesObj = pomodoroSessionMapper.sumMinutesByUserIdAndDateRange(userId, todayStart, now);
        int todayMinutes = todayMinutesObj != null ? todayMinutesObj : 0;

        int streakDays = calculateStreakDays(userId);

        return new PomodoroStatsDto(totalSessions, completedSessions, totalMinutes, todaySessions, todayMinutes, streakDays);
    }

    @Override
    public List<PomodoroSession> getHistory(String userId, int page, int size) {
        int offset = (page - 1) * size;
        return pomodoroSessionMapper.findByUserIdPaged(userId, offset, size);
    }

    private int calculateStreakDays(String userId) {
        List<String> dates = pomodoroSessionMapper.findDistinctCompletedDates(userId);
        if (dates.isEmpty()) return 0;

        int streak = 0;
        LocalDate expected = LocalDate.now();

        for (String dateStr : dates) {
            LocalDate date = LocalDate.parse(dateStr);
            if (date.equals(expected)) {
                streak++;
                expected = expected.minusDays(1);
            } else if (date.isBefore(expected)) {
                break;
            }
        }
        return streak;
    }

    @Override
    @Transactional
    public void deleteSession(Long id, String userId) {
        PomodoroSession session = pomodoroSessionMapper.findById(id);
        if (session == null) {
            throw new ResourceNotFoundException("Pomodoro session not found with id: " + id);
        }
        verifyOwnership(session.getUserId(), userId, id);
        pomodoroSessionMapper.deleteById(id);
        log.info("Pomodoro session {} deleted by user {}", id, userId);
        evictDashboardCache();
    }

    @Override
    @Transactional
    public void clearHistory(String userId) {
        pomodoroSessionMapper.deleteByUserId(userId);
        log.info("All pomodoro history cleared for user {}", userId);
        evictDashboardCache();
    }

    @Override
    public List<Map<String, Object>> getDailySummary(String userId, String date) {
        return pomodoroSessionMapper.dailySummary(userId, date);
    }

    private void verifyOwnership(String entityAuthor, String currentUser, Long entityId) {
        if (!currentUser.equals(entityAuthor)) {
            throw new AccessDeniedException("No permission to access pomodoro session " + entityId);
        }
    }
}
