package com.mindpulse.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindpulse.backend.dto.TaskDto;
import com.mindpulse.backend.entity.Task;
import com.mindpulse.backend.mapper.TaskMapper;
import com.mindpulse.backend.util.DistributedLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private DistributedLock distributedLock;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private TaskService taskService;

    private Task sampleTask;
    private TaskDto sampleTaskDto;

    @BeforeEach
    void setUp() {
        sampleTask = new Task();
        sampleTask.setId(1L);
        sampleTask.setTitle("Test Task");
        sampleTask.setDescription("Test Description");
        sampleTask.setAuthor("testuser");
        sampleTask.setPriority("medium");
        sampleTask.setStatus("pending");
        sampleTask.setVersion(0);
        sampleTask.setCreatedAt(LocalDateTime.now());
        sampleTask.setUpdatedAt(LocalDateTime.now());

        sampleTaskDto = new TaskDto(
                null, "Test Task", "Test Description", null,
                "medium", "pending", "testuser", null, null
        );
    }

    // --- createTask ---

    @Test
    @DisplayName("should_CreateTask_When_ValidTaskDto")
    void should_CreateTask_When_ValidTaskDto() {
        // Arrange
        doAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setId(1L);
            return null;
        }).when(taskMapper).insertTask(any(Task.class));

        // Act
        Task result = taskService.createTask(sampleTaskDto);

        // Assert
        assertNotNull(result);
        assertEquals("Test Task", result.getTitle());
        assertEquals("Test Description", result.getDescription());
        assertEquals("testuser", result.getAuthor());
        assertEquals("medium", result.getPriority());
        assertEquals("pending", result.getStatus());
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getUpdatedAt());
        verify(taskMapper, times(1)).insertTask(any(Task.class));
    }

    @Test
    @DisplayName("should_SetTimestamps_When_CreatingTask")
    void should_SetTimestamps_When_CreatingTask() {
        // Arrange
        LocalDateTime before = LocalDateTime.now();
        doAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setId(2L);
            return null;
        }).when(taskMapper).insertTask(any(Task.class));

        // Act
        Task result = taskService.createTask(sampleTaskDto);

        // Assert
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getUpdatedAt());
        assertFalse(result.getCreatedAt().isBefore(before));
    }

    // --- getAllTasksByUser ---

    @Test
    @DisplayName("should_ReturnTasks_When_ValidUserId")
    void should_ReturnTasks_When_ValidUserId() {
        // Arrange
        Task task2 = new Task();
        task2.setId(2L);
        task2.setTitle("Task 2");
        task2.setAuthor("testuser");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(taskMapper.findByAuthor("testuser")).thenReturn(List.of(sampleTask, task2));

        // Act
        List<Task> result = taskService.getAllTasksByUser("testuser");

        // Assert
        assertEquals(2, result.size());
        assertEquals("Test Task", result.get(0).getTitle());
        assertEquals("Task 2", result.get(1).getTitle());
        verify(taskMapper, times(1)).findByAuthor("testuser");
    }

    @Test
    @DisplayName("should_ReturnEmptyList_When_NoTasksForUser")
    void should_ReturnEmptyList_When_NoTasksForUser() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(taskMapper.findByAuthor("emptyuser")).thenReturn(List.of());

        // Act
        List<Task> result = taskService.getAllTasksByUser("emptyuser");

        // Assert
        assertTrue(result.isEmpty());
        verify(taskMapper, times(1)).findByAuthor("emptyuser");
    }

    @Test
    @DisplayName("should_ReturnCachedTasks_When_CacheHit")
    void should_ReturnCachedTasks_When_CacheHit() throws Exception {
        // Arrange
        String cachedJson = "[{\"id\":1,\"title\":\"Cached Task\"}]";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("tasks:user_testuser")).thenReturn(cachedJson);
        when(objectMapper.readValue(eq(cachedJson), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(List.of(sampleTask));

        // Act
        List<Task> result = taskService.getAllTasksByUser("testuser");

        // Assert
        assertEquals(1, result.size());
        verify(taskMapper, never()).findByAuthor(anyString());
    }

    // --- updateTaskStatusWithLock ---

    @Test
    @DisplayName("should_UpdateStatus_When_NoConflict")
    void should_UpdateStatus_When_NoConflict() {
        // Arrange
        when(taskMapper.findById(1L)).thenReturn(sampleTask);
        when(taskMapper.updateStatusWithOptimisticLock(
                eq(1L), eq("completed"), eq(0), eq("testuser"), any(LocalDateTime.class)))
                .thenReturn(1);

        // Act
        Task result = taskService.updateTaskStatusWithLock(1L, "completed", "testuser");

        // Assert
        assertNotNull(result);
        assertEquals("completed", result.getStatus());
        assertEquals(1, result.getVersion());
        verify(taskMapper, times(1)).updateStatusWithOptimisticLock(
                eq(1L), eq("completed"), eq(0), eq("testuser"), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("should_ReturnNull_When_OptimisticLockConflict")
    void should_ReturnNull_When_OptimisticLockConflict() {
        // Arrange
        when(taskMapper.findById(1L)).thenReturn(sampleTask);
        when(taskMapper.updateStatusWithOptimisticLock(
                eq(1L), eq("completed"), eq(0), eq("testuser"), any(LocalDateTime.class)))
                .thenReturn(0);

        // Act
        Task result = taskService.updateTaskStatusWithLock(1L, "completed", "testuser");

        // Assert
        assertNull(result);
    }

    @Test
    @DisplayName("should_ReturnNull_When_TaskNotFound")
    void should_ReturnNull_When_TaskNotFound() {
        // Arrange
        when(taskMapper.findById(999L)).thenReturn(null);

        // Act
        Task result = taskService.updateTaskStatusWithLock(999L, "completed", "testuser");

        // Assert
        assertNull(result);
        verify(taskMapper, never()).updateStatusWithOptimisticLock(anyLong(), anyString(), anyInt(), anyString(), any());
    }

    @Test
    @DisplayName("should_ThrowAccessDenied_When_UpdateStatusByNonOwner")
    void should_ThrowAccessDenied_When_UpdateStatusByNonOwner() {
        // Arrange
        when(taskMapper.findById(1L)).thenReturn(sampleTask);

        // Act & Assert
        assertThrows(AccessDeniedException.class,
                () -> taskService.updateTaskStatusWithLock(1L, "completed", "otheruser"));
    }

    // --- deleteTask ---

    @Test
    @DisplayName("should_DeleteTask_When_TaskExists")
    void should_DeleteTask_When_TaskExists() {
        // Arrange
        when(taskMapper.findById(1L)).thenReturn(sampleTask);

        // Act
        taskService.deleteTask(1L);

        // Assert
        verify(taskMapper, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("should_DeleteTask_When_TaskNotFound")
    void should_DeleteTask_When_TaskNotFound() {
        // Arrange
        when(taskMapper.findById(999L)).thenReturn(null);

        // Act
        taskService.deleteTask(999L);

        // Assert
        verify(taskMapper, times(1)).deleteById(999L);
    }

    // --- getTaskById ---

    @Test
    @DisplayName("should_ReturnTask_When_TaskExists")
    void should_ReturnTask_When_TaskExists() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(taskMapper.findById(1L)).thenReturn(sampleTask);

        // Act
        Optional<Task> result = taskService.getTaskById(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals("Test Task", result.get().getTitle());
        verify(taskMapper, times(1)).findById(1L);
    }

    @Test
    @DisplayName("should_ReturnEmpty_When_TaskNotFound")
    void should_ReturnEmpty_When_TaskNotFound() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(taskMapper.findById(999L)).thenReturn(null);

        // Act
        Optional<Task> result = taskService.getTaskById(999L);

        // Assert
        assertTrue(result.isEmpty());
    }

    // --- verifyOwnership ---

    @Test
    @DisplayName("should_PassVerification_When_OwnerMatches")
    void should_PassVerification_When_OwnerMatches() {
        // Act & Assert - should not throw
        assertDoesNotThrow(() -> taskService.verifyOwnership("testuser", "testuser", 1L));
    }

    @Test
    @DisplayName("should_ThrowAccessDenied_When_OwnerDoesNotMatch")
    void should_ThrowAccessDenied_When_OwnerDoesNotMatch() {
        // Act & Assert
        assertThrows(AccessDeniedException.class,
                () -> taskService.verifyOwnership("testuser", "otheruser", 1L));
    }

    // --- getTasksByUserAndStatus ---

    @Test
    @DisplayName("should_ReturnFilteredTasks_When_StatusProvided")
    void should_ReturnFilteredTasks_When_StatusProvided() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(taskMapper.findByAuthorAndStatus("testuser", "pending")).thenReturn(List.of(sampleTask));

        // Act
        List<Task> result = taskService.getTasksByUserAndStatus("testuser", "pending");

        // Assert
        assertEquals(1, result.size());
        assertEquals("pending", result.get(0).getStatus());
        verify(taskMapper, times(1)).findByAuthorAndStatus("testuser", "pending");
    }
}
