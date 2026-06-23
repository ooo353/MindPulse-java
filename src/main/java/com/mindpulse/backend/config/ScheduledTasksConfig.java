package com.mindpulse.backend.config;

import com.mindpulse.backend.entity.Reminder;
import com.mindpulse.backend.entity.Task;
import com.mindpulse.backend.service.IReminderService;
import com.mindpulse.backend.service.ITaskService;
import com.mindpulse.backend.util.DistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class ScheduledTasksConfig {

    private final ITaskService taskService;
    private final IReminderService reminderService;
    private final SimpMessagingTemplate messagingTemplate;
    private final DistributedLock distributedLock;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Scheduled(fixedRate = 60000)
    public void checkAndFireReminders() {
        List<Reminder> enabledReminders = reminderService.findAllEnabled();
        if (enabledReminders.isEmpty()) return;

        LocalTime now = LocalTime.now().withSecond(0).withNano(0);
        LocalDate today = LocalDate.now();
        DayOfWeek todayDow = today.getDayOfWeek();

        for (Reminder reminder : enabledReminders) {
            if (!shouldFire(reminder, now, today, todayDow)) continue;

            String lockKey = "reminder:" + reminder.getId() + ":" + today + ":" + now.getHour() + ":" + now.getMinute();
            String lockValue = distributedLock.tryLock(lockKey, 120);
            if (lockValue == null) continue;

            try {
                fireReminder(reminder);
                // Disable ONCE reminders after firing
                if ("ONCE".equals(reminder.getRemindType())) {
                    reminderService.disableReminder(reminder.getId());
                    log.info("ONCE reminder disabled after firing: id={}", reminder.getId());
                }
            } finally {
                distributedLock.unlock(lockKey, lockValue);
            }
        }
    }

    @Scheduled(fixedRate = 600000)
    public void checkUpcomingTasks() {
        String windowKey = LocalTime.now().withSecond(0).withNano(0).format(TIME_FMT);
        String lockKey = "scheduler:task-reminder:" + windowKey;
        String lockValue = distributedLock.tryLock(lockKey, 600);
        if (lockValue == null) {
            log.debug("Task due scan already executed by another instance");
            return;
        }

        try {
            log.info("Scanning upcoming tasks...");
            List<Task> upcomingTasks = taskService.findPendingTasksNearDueDate();

            for (Task task : upcomingTasks) {
                Map<String, Object> data = new HashMap<>();
                data.put("type", "TASK_DUE");
                data.put("taskId", task.getId());
                data.put("title", task.getTitle());
                data.put("dueDate", task.getDueDate() != null
                        ? task.getDueDate().format(DATE_FMT) : null);
                data.put("message", "Task '" + task.getTitle() + "' is due soon!");
                data.put("author", task.getAuthor());

                messagingTemplate.convertAndSendToUser(task.getAuthor(), "/queue/reminders", data);
                log.info("Task due reminder pushed: taskId={}, user={}", task.getId(), task.getAuthor());
            }
        } finally {
            distributedLock.unlock(lockKey, lockValue);
        }
    }

    private boolean shouldFire(Reminder r, LocalTime now, LocalDate today, DayOfWeek todayDow) {
        if (r.getRemindTime() == null) return false;

        // Use range check: allow firing if current minute matches (handles scheduler jitter)
        LocalTime remindTime = r.getRemindTime().withSecond(0).withNano(0);
        if (!remindTime.equals(now)) return false;

        return switch (r.getRemindType()) {
            case "ONCE" -> r.getRemindDate() != null && r.getRemindDate().equals(today);
            case "DAILY" -> true;
            case "WEEKLY" -> r.getDayOfWeek() != null
                    && r.getDayOfWeek().equalsIgnoreCase(todayDow.name().substring(0, 3));
            case "CUSTOM" -> evaluateCron(r.getCronExpression(), today, todayDow);
            default -> false;
        };
    }

    /**
     * Evaluate a simple cron expression against the current date.
     * Supports standard 5-field cron: minute hour day-of-month month day-of-week
     */
    private boolean evaluateCron(String cronExpression, LocalDate today, DayOfWeek todayDow) {
        if (!StringUtils.hasText(cronExpression)) return true; // No cron = fire every day

        try {
            // Use Spring's built-in cron parser
            org.springframework.scheduling.support.CronExpression cron =
                    org.springframework.scheduling.support.CronExpression.parse(cronExpression);
            // Check if the cron matches the current minute (next() returns next match time)
            java.time.LocalDateTime now = java.time.LocalDateTime.of(today, LocalTime.now().withSecond(0).withNano(0));
            java.time.LocalDateTime nextMatch = cron.next(now.minusMinutes(1));
            return nextMatch != null && nextMatch.toLocalDate().equals(today)
                    && nextMatch.getHour() == now.getHour() && nextMatch.getMinute() == now.getMinute();
        } catch (Exception e) {
            log.warn("Invalid cron expression '{}', treating as DAILY: {}", cronExpression, e.getMessage());
            return true; // Fallback to daily if cron is invalid
        }
    }

    private void fireReminder(Reminder reminder) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "REMINDER");
        data.put("reminderId", reminder.getId());
        data.put("message", reminder.getMessage());
        data.put("remindTime", reminder.getRemindTime().format(TIME_FMT));
        data.put("targetId", reminder.getTargetId());
        data.put("targetType", reminder.getTargetType());

        messagingTemplate.convertAndSendToUser(reminder.getUserId(), "/queue/reminders", data);
        log.info("Reminder pushed: id={}, user={}", reminder.getId(), reminder.getUserId());
    }
}
