package com.mindpulse.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindpulse.backend.dto.TaskDto;
import com.mindpulse.backend.entity.Task;
import com.mindpulse.backend.mapper.TaskMapper;
import com.mindpulse.backend.util.DistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService implements ITaskService {

    private final TaskMapper taskMapper;
    private final DistributedLock distributedLock;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CACHE_PREFIX = "tasks:";
    private static final long CACHE_TTL_MINUTES = 10;

    // --- Manual cache helpers ---

    private <T> T getFromCache(String key, TypeReference<T> typeRef) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                return objectMapper.readValue(json, typeRef);
            }
        } catch (Exception e) {
            log.warn("Redis cache read failed for key={}: {}", key, e.getMessage());
        }
        return null;
    }

    private void putToCache(String key, Object value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Redis cache write failed for key={}: {}", key, e.getMessage());
        }
    }

    private void evictCache(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Redis cache evict failed for key={}: {}", key, e.getMessage());
        }
    }

    private void evictCacheByPattern(String pattern) {
        try {
            var keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("Redis cache evict failed for pattern={}: {}", pattern, e.getMessage());
        }
    }

    // --- Read operations with cache-aside ---

    @Override
    public List<Task> getAllTasksByUser(String username) {
        String key = CACHE_PREFIX + "user_" + username;
        List<Task> cached = getFromCache(key, new TypeReference<List<Task>>() {});
        if (cached != null) {
            return cached;
        }
        List<Task> tasks = taskMapper.findByAuthor(username);
        putToCache(key, tasks);
        return tasks;
    }

    @Override
    public List<Task> getTasksByUserAndStatus(String username, String status) {
        String key = CACHE_PREFIX + "user_" + username + "_status_" + status;
        List<Task> cached = getFromCache(key, new TypeReference<List<Task>>() {});
        if (cached != null) {
            return cached;
        }
        List<Task> tasks = taskMapper.findByAuthorAndStatus(username, status);
        putToCache(key, tasks);
        return tasks;
    }

    @Override
    public Optional<Task> getTaskById(Long id) {
        String key = CACHE_PREFIX + "id_" + id;
        Task cached = getFromCache(key, new TypeReference<Task>() {});
        if (cached != null) {
            return Optional.of(cached);
        }
        Task task = taskMapper.findById(id);
        if (task != null) {
            putToCache(key, task);
            return Optional.of(task);
        }
        return Optional.empty();
    }

    // --- Write operations with cache eviction ---

    @Override
    @Transactional
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

        evictCacheByPattern(CACHE_PREFIX + "user_" + taskDto.author() + "*");
        return task;
    }

    @Override
    @Transactional
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

        evictCacheByPattern(CACHE_PREFIX + "user_" + author + "*");
        return task;
    }

    @Override
    @Transactional
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

            evictCache(CACHE_PREFIX + "id_" + id);
            evictCacheByPattern(CACHE_PREFIX + "user_" + existingTask.getAuthor() + "*");
            return existingTask;
        } else {
            throw new RuntimeException("Task not found with id: " + id);
        }
    }

    @Override
    @Transactional
    public void deleteTask(Long id) {
        Task task = taskMapper.findById(id);
        taskMapper.deleteById(id);

        evictCache(CACHE_PREFIX + "id_" + id);
        if (task != null) {
            evictCacheByPattern(CACHE_PREFIX + "user_" + task.getAuthor() + "*");
        }
    }

    @Override
    public List<Task> findPendingTasksNearDueDate() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime futureTime = now.plusMinutes(30);
        return taskMapper.findPendingTasksNearDueDate(now, futureTime);
    }

    @Override
    @Transactional
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

            evictCache(CACHE_PREFIX + "id_" + id);
            evictCacheByPattern(CACHE_PREFIX + "user_" + username + "*");
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
