package com.mindpulse.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindpulse.backend.dto.TaskDto;
import com.mindpulse.backend.entity.Task;
import com.mindpulse.backend.exception.GlobalExceptionHandler;
import com.mindpulse.backend.exception.ResourceNotFoundException;
import com.mindpulse.backend.service.AiAgentClient;
import com.mindpulse.backend.service.TaskService;
import com.mindpulse.backend.service.ai.SemanticCacheService;
import com.mindpulse.backend.service.ai.TaskValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TaskControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private TaskService taskService;

    @Mock
    private AiAgentClient aiAgentClient;

    @Mock
    private SemanticCacheService semanticCacheService;

    @Mock
    private TaskValidationService taskValidationService;

    @InjectMocks
    private TaskController taskController;

    private Task sampleTask;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(taskController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

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

        // Set up security context with authenticated user
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("testuser", null, List.of()));
    }

    // --- GET /api/tasks ---

    @Test
    @DisplayName("should_Return200_When_GetAllTasks")
    void should_Return200_When_GetAllTasks() throws Exception {
        // Arrange
        Task task2 = new Task();
        task2.setId(2L);
        task2.setTitle("Task 2");
        task2.setAuthor("testuser");
        task2.setStatus("pending");

        when(taskService.getAllTasksByUser("testuser")).thenReturn(List.of(sampleTask, task2));

        // Act & Assert
        mockMvc.perform(get("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].title").value("Test Task"))
                .andExpect(jsonPath("$.data[1].title").value("Task 2"));

        verify(taskService, times(1)).getAllTasksByUser("testuser");
    }

    @Test
    @DisplayName("should_ReturnFilteredTasks_When_StatusProvided")
    void should_ReturnFilteredTasks_When_StatusProvided() throws Exception {
        // Arrange
        when(taskService.getTasksByUserAndStatus("testuser", "pending"))
                .thenReturn(List.of(sampleTask));

        // Act & Assert
        mockMvc.perform(get("/api/tasks")
                        .param("status", "pending")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].status").value("pending"));

        verify(taskService, times(1)).getTasksByUserAndStatus("testuser", "pending");
    }

    // --- POST /api/tasks ---

    @Test
    @DisplayName("should_Return201_When_CreateTask")
    void should_Return201_When_CreateTask() throws Exception {
        // Arrange
        TaskDto taskDto = new TaskDto(
                null, "New Task", "New Description", null,
                "high", "pending", "testuser", null, null
        );

        Task createdTask = new Task();
        createdTask.setId(1L);
        createdTask.setTitle("New Task");
        createdTask.setDescription("New Description");
        createdTask.setAuthor("testuser");
        createdTask.setPriority("high");
        createdTask.setStatus("pending");
        createdTask.setCreatedAt(LocalDateTime.now());
        createdTask.setUpdatedAt(LocalDateTime.now());

        when(taskService.createTask(any(TaskDto.class))).thenReturn(createdTask);

        // Act & Assert
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.title").value("New Task"))
                .andExpect(jsonPath("$.data.author").value("testuser"));

        verify(taskService, times(1)).createTask(any(TaskDto.class));
    }

    @Test
    @DisplayName("should_Return400_When_CreateTaskWithMissingTitle")
    void should_Return400_When_CreateTaskWithMissingTitle() throws Exception {
        // Arrange
        TaskDto taskDto = new TaskDto(
                null, null, "Description", null,
                "medium", "pending", "testuser", null, null
        );

        // Act & Assert
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskDto)))
                .andExpect(status().isBadRequest());
    }

    // --- PUT /api/tasks/{id}/status ---

    @Test
    @DisplayName("should_Return200_When_UpdateTaskStatus")
    void should_Return200_When_UpdateTaskStatus() throws Exception {
        // Arrange
        Task updatedTask = new Task();
        updatedTask.setId(1L);
        updatedTask.setTitle("Test Task");
        updatedTask.setAuthor("testuser");
        updatedTask.setStatus("completed");
        updatedTask.setVersion(1);

        when(taskService.updateTaskStatusWithLock(1L, "completed", "testuser"))
                .thenReturn(updatedTask);

        // Act & Assert
        mockMvc.perform(put("/api/tasks/{id}/status", 1L)
                        .param("status", "completed")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.task.status").value("completed"))
                .andExpect(jsonPath("$.data.task.version").value(1));

        verify(taskService, times(1)).updateTaskStatusWithLock(1L, "completed", "testuser");
    }

    @Test
    @DisplayName("should_Return409_When_UpdateTaskStatusConflict")
    void should_Return409_When_UpdateTaskStatusConflict() throws Exception {
        // Arrange
        when(taskService.updateTaskStatusWithLock(1L, "completed", "testuser"))
                .thenReturn(null);

        // Act & Assert
        mockMvc.perform(put("/api/tasks/{id}/status", 1L)
                        .param("status", "completed")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(409));

        verify(taskService, times(1)).updateTaskStatusWithLock(1L, "completed", "testuser");
    }

    @Test
    @DisplayName("should_Return400_When_UpdateTaskStatusInvalidValue")
    void should_Return400_When_UpdateTaskStatusInvalidValue() throws Exception {
        // Act & Assert
        mockMvc.perform(put("/api/tasks/{id}/status", 1L)
                        .param("status", "invalid_status")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verify(taskService, never()).updateTaskStatusWithLock(anyLong(), anyString(), anyString());
    }

    // --- GET /api/tasks/{id} ---

    @Test
    @DisplayName("should_Return200_When_GetTaskById")
    void should_Return200_When_GetTaskById() throws Exception {
        // Arrange
        when(taskService.getTaskById(1L)).thenReturn(Optional.of(sampleTask));

        // Act & Assert
        mockMvc.perform(get("/api/tasks/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Test Task"))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("should_Return404_When_TaskNotFound")
    void should_Return404_When_TaskNotFound() throws Exception {
        // Arrange
        when(taskService.getTaskById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/tasks/{id}", 999L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // --- DELETE /api/tasks/{id} ---

    @Test
    @DisplayName("should_Return200_When_DeleteTask")
    void should_Return200_When_DeleteTask() throws Exception {
        // Arrange
        when(taskService.getTaskById(1L)).thenReturn(Optional.of(sampleTask));
        doNothing().when(taskService).deleteTask(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/tasks/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(taskService, times(1)).deleteTask(1L);
    }
}
