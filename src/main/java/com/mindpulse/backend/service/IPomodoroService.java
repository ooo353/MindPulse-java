package com.mindpulse.backend.service;

import com.mindpulse.backend.dto.PomodoroSessionDto;
import com.mindpulse.backend.dto.PomodoroStatsDto;
import com.mindpulse.backend.entity.PomodoroSession;

import java.util.List;
import java.util.Optional;

public interface IPomodoroService {

    PomodoroSession startSession(PomodoroSessionDto dto, String userId);

    PomodoroSession completeSession(Long id, String userId);

    PomodoroSession cancelSession(Long id, String userId);

    Optional<PomodoroSession> getActiveSession(String userId);

    PomodoroStatsDto getStats(String userId, String period);

    List<PomodoroSession> getHistory(String userId, int page, int size);
}
