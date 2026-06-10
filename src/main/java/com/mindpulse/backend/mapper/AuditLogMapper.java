package com.mindpulse.backend.mapper;

import com.mindpulse.backend.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface AuditLogMapper {

    AuditLog findById(@Param("id") Long id);

    List<AuditLog> findByUserId(@Param("userId") String userId);

    List<AuditLog> findWithFilters(
        @Param("userId") String userId,
        @Param("action") String action,
        @Param("resourceType") String resourceType,
        @Param("offset") int offset,
        @Param("limit") int limit
    );

    int countWithFilters(
        @Param("userId") String userId,
        @Param("action") String action,
        @Param("resourceType") String resourceType
    );

    void insert(AuditLog auditLog);

    int countByDate(@Param("date") LocalDate date);
}
