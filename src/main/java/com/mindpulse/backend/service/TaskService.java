package com.mindpulse.backend.service;

import com.mindpulse.backend.dto.TaskDto;
import com.mindpulse.backend.entity.Task;
import com.mindpulse.backend.mapper.TaskMapper;
import com.mindpulse.backend.util.DistributedLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private DistributedLock distributedLock;

    @CacheEvict(value = "tasks", key = "#result.id")
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
        log.info("任务创建成功: id={}, title={}", task.getId(), task.getTitle());
        return task;
    }

    /**
     * 从 AI 解析结果创建任务
     */
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

        // 解析截止时间
        String dueDateStr = (String) parsedData.get("due_date");
        if (dueDateStr != null && !dueDateStr.isBlank()) {
            try {
                dueDateStr = dueDateStr.replace(" ", "T");
                task.setDueDate(LocalDateTime.parse(dueDateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            } catch (DateTimeParseException e) {
                log.warn("截止时间解析失败: {}", dueDateStr);
            }
        }

        taskMapper.insertTask(task);
        log.info("AI 解析任务创建成功: id={}, title={}, category={}", task.getId(), task.getTitle(), task.getCategory());
        return task;
    }

    @Cacheable(value = "tasks", key = "'user_'.concat(#username)")
    public List<Task> getAllTasksByUser(String username) {
        return taskMapper.findByAuthor(username);
    }

    @Cacheable(value = "tasks", key = "'user_'.concat(#username).concat('_status_').concat(#status)")
    public List<Task> getTasksByUserAndStatus(String username, String status) {
        return taskMapper.findByAuthorAndStatus(username, status);
    }

    @Cacheable(value = "tasks", key = "#id")
    public Optional<Task> getTaskById(Long id) {
        Task task = taskMapper.findById(id);
        return task != null ? Optional.of(task) : Optional.empty();
    }

    @CacheEvict(value = "tasks", key = "#id")
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

    @CacheEvict(value = "tasks", key = "#id")
    public void deleteTask(Long id) {
        taskMapper.deleteById(id);
    }

    public List<Task> findPendingTasksNearDueDate() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime futureTime = now.plusMinutes(30);
        return taskMapper.findPendingTasksNearDueDate(now, futureTime);
    }

    /**
     * 带分布式锁的任务状态更新，解决多端并发状态覆盖问题
     *
     * @param id     任务ID
     * @param status 新状态
     * @param username 操作者用户名
     * @return 更新后的任务，锁竞争失败返回 null
     */
    public Task updateTaskStatusWithLock(Long id, String status, String username) {
        String lockKey = "task:" + id;
        String lockValue = distributedLock.tryLock(lockKey, 10);
        if (lockValue == null) {
            log.warn("任务状态更新锁竞争失败: taskId={}, user={}", id, username);
            return null;
        }

        try {
            Task task = taskMapper.findById(id);
            if (task == null) {
                throw new RuntimeException("Task not found: " + id);
            }
            if (!task.getAuthor().equals(username)) {
                throw new RuntimeException("Access denied: not the task owner");
            }
            task.setStatus(status);
            task.setUpdatedAt(LocalDateTime.now());
            taskMapper.updateTask(task);
            log.info("任务状态更新成功: taskId={}, status={}, user={}", id, status, username);
            return task;
        } finally {
            distributedLock.unlock(lockKey, lockValue);
        }
    }
}
