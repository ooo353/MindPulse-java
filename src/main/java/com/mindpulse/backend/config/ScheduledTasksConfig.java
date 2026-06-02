package com.mindpulse.backend.config;

import com.mindpulse.backend.entity.Reminder;
import com.mindpulse.backend.entity.Task;
import com.mindpulse.backend.service.ReminderService;
import com.mindpulse.backend.service.TaskService;
import com.mindpulse.backend.util.DistributedLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 动态提醒调度引擎：每分钟扫描待触发提醒，Redis 分布式锁防多实例重复推送
 */
@Configuration
@EnableScheduling
public class ScheduledTasksConfig {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTasksConfig.class);

    @Autowired
    private TaskService taskService;

    @Autowired
    private ReminderService reminderService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private DistributedLock distributedLock;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * 每分钟扫描用户配置的动态提醒，匹配当前时间的提醒推送
     * 分布式锁确保同一提醒同一分钟窗口仅推送一次
     */
    @Scheduled(fixedRate = 60000)
    public void checkAndFireReminders() {
        List<Reminder> enabledReminders = reminderService.findAllEnabled();
        if (enabledReminders.isEmpty()) return;

        LocalTime now = LocalTime.now().withSecond(0).withNano(0);
        LocalDate today = LocalDate.now();
        DayOfWeek todayDow = today.getDayOfWeek();

        for (Reminder reminder : enabledReminders) {
            if (!shouldFire(reminder, now, today, todayDow)) continue;

            String lockKey = "reminder:" + reminder.getId() + ":" + now.getHour() + ":" + now.getMinute();
            String lockValue = distributedLock.tryLock(lockKey, 50);
            if (lockValue == null) continue;

            try {
                fireReminder(reminder);
            } finally {
                distributedLock.unlock(lockKey, lockValue);
            }
        }
    }

    /**
     * 每10分钟扫描即将到期任务（未来30分钟内），推送任务到期提醒
     * 分钟窗口分布式锁防止多实例重复执行
     */
    @Scheduled(fixedRate = 600000)
    public void checkUpcomingTasks() {
        String windowKey = LocalTime.now().withSecond(0).withNano(0).format(TIME_FMT);
        String lockKey = "scheduler:task-reminder:" + windowKey;
        String lockValue = distributedLock.tryLock(lockKey, 500);
        if (lockValue == null) {
            log.debug("任务到期扫描已由其他实例执行");
            return;
        }

        try {
            log.info("扫描即将到期任务...");
            List<Task> upcomingTasks = taskService.findPendingTasksNearDueDate();

            for (Task task : upcomingTasks) {
                Map<String, Object> data = new HashMap<>();
                data.put("type", "TASK_DUE");
                data.put("taskId", task.getId());
                data.put("title", task.getTitle());
                data.put("dueDate", task.getDueDate() != null
                        ? task.getDueDate().format(DATE_FMT) : null);
                data.put("message", "任务「" + task.getTitle() + "」即将到期！");
                data.put("author", task.getAuthor());

                messagingTemplate.convertAndSendToUser(task.getAuthor(), "/queue/reminders", data);
                log.info("任务到期提醒已推送: taskId={}, user={}", task.getId(), task.getAuthor());
            }
        } finally {
            distributedLock.unlock(lockKey, lockValue);
        }
    }

    private boolean shouldFire(Reminder r, LocalTime now, LocalDate today, DayOfWeek todayDow) {
        if (r.getRemindTime() == null) return false;
        LocalTime remindTime = r.getRemindTime().withSecond(0).withNano(0);
        if (!remindTime.equals(now)) return false;

        return switch (r.getRemindType()) {
            case "ONCE" -> r.getRemindDate() == null || r.getRemindDate().equals(today);
            case "DAILY" -> true;
            case "WEEKLY" -> r.getDayOfWeek() != null
                    && r.getDayOfWeek().equalsIgnoreCase(todayDow.name().substring(0, 3));
            case "CUSTOM" -> true;
            default -> false;
        };
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
        log.info("提醒已推送: id={}, user={}", reminder.getId(), reminder.getUserId());
    }
}
