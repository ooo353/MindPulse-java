package com.mindpulse.backend.mapper;

import com.mindpulse.backend.entity.Reminder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReminderMapper {

    Reminder findById(@Param("id") Long id);

    List<Reminder> findByUserId(@Param("userId") String userId);

    List<Reminder> findAllEnabled();

    void insertReminder(Reminder reminder);

    void updateReminder(Reminder reminder);

    void deleteById(@Param("id") Long id);

    void disableById(@Param("id") Long id);
}
