package com.mindpulse.backend.service;

import com.mindpulse.backend.dto.TaskDto;
import com.mindpulse.backend.entity.Task;
import com.mindpulse.backend.mapper.TaskMapper;
import com.mindpulse.backend.util.DistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService implements ITaskService {

    private final TaskMapper taskMapper;
    private final DistributedLock distributedLock;

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "tasks", allEntries = true),
        @CacheEvict(value = "dashboard", allEntries = true)
    })
    public Task createTask(TaskDto taskDto) {
        Task task = new Task();
        task.setTitle(taskDto.title());
        task.setDescription(taskDto.description());
        task.setDueDate(taskDto.dueDate());
        task.setPriority(taskDto.priority());
        task.setStatus(taskDto.status());
        task.setCategory(taskDto.category());
        task.setAuthor(taskDto.author());
        task.setRelatedNotes(taskDto.relatedNotes());
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());

        taskMapper.insertTask(task);
        log.info("Task created: id={}, title={}", task.getId(), task.getTitle());
        return task;
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "tasks", allEntries = true),
        @CacheEvict(value = "dashboard", allEntries = true)
    })
    public Task createFromParsedData(Map<String, Object> parsedData, String author) {
        Task task = new Task();
        task.setTitle((String) parsedData.get("title"));
        task.setDescription((String) parsedData.get("description"));
        task.setPriority((String) parsedData.get("priority"));
        task.setStatus("pending");
        task.setCategory((String) parsedData.get("category"));
        task.setAuthor(author);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());

        String dueDateStr = (String) parsedData.get("due_date");
        if (dueDateStr != null && !dueDateStr.isBlank()) {
            try {
                dueDateStr = dueDateStr.replace(" ", "T");
                task.setDueDate(LocalDateTime.parse(dueDateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            } catch (DateTimeParseException e) {
                log.warn("Failed to parse due date: {}", dueDateStr);
            }
        }

        taskMapper.insertTask(task);
        log.info("AI-parsed task created: id={}, title={}, category={}", task.getId(), task.getTitle(), task.getCategory());
        return task;
    }

    @Override
    @Cacheable(value = "tasks", key = "'user_'.concat(#username)")
    public List<Task> getAllTasksByUser(String username) {
        return taskMapper.findByAuthor(username);
    }

    @Override
    @Cacheable(value = "tasks", key = "'user_'.concat(#username).concat('_status_').concat(#status)")
    public List<Task> getTasksByUserAndStatus(String username, String status) {
        return taskMapper.findByAuthorAndStatus(username, status);
    }

    @Override
    @Cacheable(value = "tasks", key = "#id")
    public Optional<Task> getTaskById(Long id) {
        Task task = taskMapper.findById(id);
        return task != null ? Optional.of(task) : Optional.empty();
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "tasks", allEntries = true),
        @CacheEvict(value = "dashboard", allEntries = true)
    })
    public Task updateTask(Long id, TaskDto taskDto) {
        Task existingTask = taskMapper.findById(id);

        if (existingTask != null) {
            existingTask.setTitle(taskDto.title());
            existingTask.setDescription(taskDto.description());
            existingTask.setDueDate(taskDto.dueDate());
            existingTask.setPriority(taskDto.priority());
            existingTask.setStatus(taskDto.status());
            existingTask.setCategory(taskDto.category());
            existingTask.setRelatedNotes(taskDto.relatedNotes());
            existingTask.setUpdatedAt(LocalDateTime.now());

            taskMapper.updateTask(existingTask);
            return existingTask;
        } else {
            throw new RuntimeException("Task not found with id: " + id);
        }
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "tasks", allEntries = true),
        @CacheEvict(value = "dashboard", allEntries = true)
    })
    public void deleteTask(Long id) {
        taskMapper.deleteById(id);
    }

    @Override
    public List<Task> findPendingTasksNearDueDate() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime futureTime = now.plusMinutes(30);
        return taskMapper.findPendingTasksNearDueDate(now, futureTime);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "tasks", allEntries = true),
        @CacheEvict(value = "dashboard", allEntries = true)
    })
    public Task updateTaskStatusWithLock(Long id, String status, String username) {
        String lockKey = "task:" + id;
        String lockValue = distributedLock.tryLock(lockKey, 10);
        if (lockValue == null) {
            log.warn("Task status update lock contention failed: taskId={}, user={}", id, username);
            return null;
        }

        try {
            Task task = taskMapper.findById(id);
            if (task == null) {
                throw new RuntimeException("Task not found: " + id);
            }
            verifyOwnership(task.getAuthor(), username, id);
            task.setStatus(status);
            task.setUpdatedAt(LocalDateTime.now());
            taskMapper.updateTask(task);
            log.info("Task status updated: taskId={}, status={}, user={}", id, status, username);
            return task;
        } finally {
            distributedLock.unlock(lockKey, lockValue);
        }
    }

    @Override
    public void verifyOwnership(String entityAuthor, String currentUser, Long entityId) {
        if (!entityAuthor.equals(currentUser)) {
            throw new AccessDeniedException("You do not have permission to access entity with id " + entityId);
        }
    }
}
