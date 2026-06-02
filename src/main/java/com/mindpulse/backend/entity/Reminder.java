package com.mindpulse.backend.entity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

public class Reminder {

    private Long id;
    private String userId;           // 提醒所属用户
    private String message;          // 提醒内容
    private String remindType;       // ONCE/DAILY/WEEKLY/CUSTOM
    private LocalTime remindTime;    // 提醒时间点
    private LocalDate remindDate;    // 一次性提醒的具体日期
    private String dayOfWeek;        // 每周提醒的星期几 (MON/TUE/...)
    private String cronExpression;   // 自定义 cron 表达式
    private Long targetId;           // 关联的目标ID（任务/笔记）
    private String targetType;       // TASK/NOTE
    private Boolean enabled = true;  // 是否启用
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
