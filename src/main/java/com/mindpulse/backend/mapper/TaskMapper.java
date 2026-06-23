package com.mindpulse.backend.mapper;

import com.mindpulse.backend.entity.Task;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TaskMapper {
    Task findById(@Param("id") Long id);
    
    List<Task> findByAuthor(@Param("username") String username);
    
    List<Task> findByAuthorAndStatus(
        @Param("username") String username,
        @Param("status") String status
    );
    
    List<Task> findPendingTasksNearDueDate(
        @Param("now") LocalDateTime now,
        @Param("futureTime") LocalDateTime futureTime
    );
    
    void insertTask(Task task);
    
    void updateTask(Task task);
    
    void deleteById(@Param("id") Long id);

    int updateStatusWithOptimisticLock(
        @Param("id") Long id,
        @Param("status") String status,
        @Param("version") Integer version,
        @Param("username") String username,
        @Param("updatedAt") LocalDateTime updatedAt
    );
}