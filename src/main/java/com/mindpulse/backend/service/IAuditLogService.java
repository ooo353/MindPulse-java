package com.mindpulse.backend.service;

import com.mindpulse.backend.dto.AdminStatsDto;
import com.mindpulse.backend.dto.AdminUserDto;
import com.mindpulse.backend.dto.AuditLogQueryDto;
import com.mindpulse.backend.entity.AuditLog;

import java.util.List;
import java.util.Map;

public interface IAuditLogService {

    Map<String, Object> getAuditLogs(AuditLogQueryDto query);

    List<AdminUserDto> getAllUsers();

    void updateUserRole(Long userId, String role);

    AdminStatsDto getAdminStats();

    void saveAuditLog(AuditLog auditLog);
}
