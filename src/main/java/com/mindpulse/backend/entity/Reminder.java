package com.mindpulse.backend.entity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

public class Reminder {

    private Long id;
    private String userId;
    private String message;
    private String remindType;       // ONCE/DAILY/WEEKLY/CUSTOM
    private LocalTime remindTime;
    private LocalDate remindDate;    // One-time reminder specific date
    private String dayOfWeek;        // Weekly reminder day (MON/TUE/...)
    private String cronExpression;   // Custom cron expression
    private Long targetId;           // Associated target ID (task/note)
    private String targetType;       // TASK/NOTE
    private Boolean enabled = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Reminder() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getRemindType() { return remindType; }
    public void setRemindType(String remindType) { this.remindType = remindType; }
    public LocalTime getRemindTime() { return remindTime; }
    public void setRemindTime(LocalTime remindTime) { this.remindTime = remindTime; }
    public LocalDate getRemindDate() { return remindDate; }
    public void setRemindDate(LocalDate remindDate) { this.remindDate = remindDate; }
    public String getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public String getCronExpression() { return cronExpression; }
    public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
