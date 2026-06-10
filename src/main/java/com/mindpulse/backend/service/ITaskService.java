package com.mindpulse.backend.service;

import com.mindpulse.backend.dto.TaskDto;
import com.mindpulse.backend.entity.Task;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ITaskService {
    Task createTask(TaskDto taskDto);
    Task createFromParsedData(Map<String, Object> parsedData, String author);
    List<Task> getAllTasksByUser(String username);
    List<Task> getTasksByUserAndStatus(String username, String status);
    Optional<Task> getTaskById(Long id);
    Task updateTask(Long id, TaskDto taskDto);
    void deleteTask(Long id);
    List<Task> findPendingTasksNearDueDate();
    Task updateTaskStatusWithLock(Long id, String status, String username);
    void verifyOwnership(String entityAuthor, String currentUser, Long entityId);
}
