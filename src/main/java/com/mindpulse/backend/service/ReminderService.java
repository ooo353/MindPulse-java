package com.mindpulse.backend.service;

import com.mindpulse.backend.dto.ReminderDto;
import com.mindpulse.backend.entity.Reminder;
import com.mindpulse.backend.exception.ResourceNotFoundException;
import com.mindpulse.backend.mapper.ReminderMapper;
import com.mindpulse.backend.util.DistributedLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReminderService {

    private static final Logger log = LoggerFactory.getLogger(ReminderService.class);

    @Autowired
    private ReminderMapper reminderMapper;

    @Autowired
    private DistributedLock distributedLock;

    public Reminder createReminder(ReminderDto dto, String username) {
        Reminder reminder = new Reminder();
        reminder.setUserId(username);
        reminder.setMessage(dto.message());
        reminder.setRemindType(dto.remindType());
        reminder.setRemindTime(dto.remindTime());
        reminder.setRemindDate(dto.remindDate());
        reminder.setDayOfWeek(dto.dayOfWeek());
        reminder.setCronExpression(dto.cronExpression());
        reminder.setTargetId(dto.targetId());
        reminder.setTargetType(dto.targetType());
        reminder.setEnabled(dto.enabled());
        reminder.setCreatedAt(LocalDateTime.now());
        reminder.setUpdatedAt(LocalDateTime.now());

        reminderMapper.insertReminder(reminder);
        log.info("提醒创建成功: id={}, type={}, user={}", reminder.getId(), reminder.getRemindType(), username);
        return reminder;
    }

    public List<Reminder> getUserReminders(String username) {
        return reminderMapper.findByUserId(username);
    }

    public Reminder getReminderById(Long id, String username) {
        Reminder reminder = reminderMapper.findById(id);
        if (reminder == null || !reminder.getUserId().equals(username)) {
            throw new ResourceNotFoundException("Reminder not found: " + id);
        }
        return reminder;
    }

    public Reminder updateReminder(Long id, ReminderDto dto, String username) {
        Reminder existing = getReminderById(id, username);
        existing.setMessage(dto.message());
        existing.setRemindType(dto.remindType());
        existing.setRemindTime(dto.remindTime());
        existing.setRemindDate(dto.remindDate());
        existing.setDayOfWeek(dto.dayOfWeek());
        existing.setCronExpression(dto.cronExpression());
        existing.setTargetId(dto.targetId());
        existing.setTargetType(dto.targetType());
        existing.setEnabled(dto.enabled() != null ? dto.enabled() : existing.getEnabled());
        existing.setUpdatedAt(LocalDateTime.now());

        reminderMapper.updateReminder(existing);
        log.info("提醒已更新: id={}", id);
        return existing;
    }

    public void deleteReminder(Long id, String username) {
        getReminderById(id, username); // 权限校验
        reminderMapper.deleteById(id);
        log.info("提醒已删除: id={}", id);
    }

    /**
     * 获取所有启用的提醒（供调度器使用）
     */
    public List<Reminder> findAllEnabled() {
        return reminderMapper.findAllEnabled();
    }
}
