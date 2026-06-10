package com.mindpulse.backend.service;

import com.mindpulse.backend.dto.ReminderDto;
import com.mindpulse.backend.entity.Reminder;

import java.util.List;

public interface IReminderService {
    Reminder createReminder(ReminderDto dto, String username);
    List<Reminder> getUserReminders(String username);
    Reminder getReminderById(Long id, String username);
    Reminder updateReminder(Long id, ReminderDto dto, String username);
    void deleteReminder(Long id, String username);
    List<Reminder> findAllEnabled();
}
