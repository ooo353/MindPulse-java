package com.mindpulse.backend.service;

import com.mindpulse.backend.dto.AdminStatsDto;
import com.mindpulse.backend.dto.AdminUserDto;
import com.mindpulse.backend.dto.AuditLogQueryDto;
import com.mindpulse.backend.entity.AuditLog;
import com.mindpulse.backend.entity.User;
import com.mindpulse.backend.exception.ResourceNotFoundException;
import com.mindpulse.backend.mapper.AuditLogMapper;
import com.mindpulse.backend.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService implements IAuditLogService {

    private final AuditLogMapper auditLogMapper;
    private final UserMapper userMapper;

    @Override
    public Map<String, Object> getAuditLogs(AuditLogQueryDto query) {
        int page = query.page();
        int size = query.size();
        int offset = (page - 1) * size;

        List<AuditLog> records = auditLogMapper.findWithFilters(
            query.userId(), query.action(), query.resourceType(), offset, size
        );
        int total = auditLogMapper.countWithFilters(
            query.userId(), query.action(), query.resourceType()
        );

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    @Override
    public List<AdminUserDto> getAllUsers() {
        return userMapper.findAll().stream()
            .map(user -> new AdminUserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt()
            ))
            .toList();
    }

    @Override
    @Transactional
    public void updateUserRole(Long userId, String role) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        userMapper.updateRole(userId, role);
        log.info("User {} role updated to {}", userId, role);
    }

    @Override
    public AdminStatsDto getAdminStats() {
        int totalUsers = userMapper.countAll();
        int totalLogs = auditLogMapper.countWithFilters(null, null, null);
        int todayActions = auditLogMapper.countByDate(LocalDate.now());
        return new AdminStatsDto(totalUsers, totalLogs, todayActions);
    }

    @Override
    public void saveAuditLog(AuditLog auditLog) {
        try {
            auditLogMapper.insert(auditLog);
        } catch (Exception e) {
            log.error("Failed to save audit log: {}", e.getMessage(), e);
        }
    }
}
