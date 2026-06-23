package com.mindpulse.backend.service;

import com.mindpulse.backend.dto.ReminderDto;
import com.mindpulse.backend.entity.Reminder;
import com.mindpulse.backend.exception.ResourceNotFoundException;
import com.mindpulse.backend.mapper.ReminderMapper;
import com.mindpulse.backend.util.DistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderService implements IReminderService {

    private final ReminderMapper reminderMapper;
    private final DistributedLock distributedLock;

    @Override
    @Transactional
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
        log.info("Reminder created: id={}, type={}, user={}", reminder.getId(), reminder.getRemindType(), username);
        return reminder;
    }

    @Override
    public List<Reminder> getUserReminders(String username) {
        return reminderMapper.findByUserId(username);
    }

    @Override
    public Reminder getReminderById(Long id, String username) {
        Reminder reminder = reminderMapper.findById(id);
        if (reminder == null || !reminder.getUserId().equals(username)) {
            throw new ResourceNotFoundException("Reminder not found: " + id);
        }
        return reminder;
    }

    @Override
    @Transactional
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
        log.info("Reminder updated: id={}", id);
        return existing;
    }

    @Override
    @Transactional
    public void deleteReminder(Long id, String username) {
        getReminderById(id, username);
        reminderMapper.deleteById(id);
        log.info("Reminder deleted: id={}", id);
    }

    @Override
    public List<Reminder> findAllEnabled() {
        return reminderMapper.findAllEnabled();
    }

    @Override
    @Transactional
    public void disableReminder(Long id) {
        reminderMapper.disableById(id);
    }
}
