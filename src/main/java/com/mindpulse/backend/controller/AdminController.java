package com.mindpulse.backend.controller;

import com.mindpulse.backend.dto.AdminStatsDto;
import com.mindpulse.backend.dto.AdminUserDto;
import com.mindpulse.backend.dto.ApiResponse;
import com.mindpulse.backend.dto.AuditLogQueryDto;
import com.mindpulse.backend.service.IAuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin", description = "Admin management endpoints")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final IAuditLogService auditLogService;

    @Operation(summary = "Get audit logs", description = "Retrieve paginated audit logs with filters")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Audit logs retrieved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required")
    })
    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAuditLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        AuditLogQueryDto query = new AuditLogQueryDto(action, resourceType, userId, page, size);
        Map<String, Object> result = auditLogService.getAuditLogs(query);
        return ResponseEntity.ok(ApiResponse.success(200, "Audit logs retrieved", result));
    }

    @Operation(summary = "Get all users", description = "Retrieve list of all registered users")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Users retrieved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required")
    })
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<AdminUserDto>>> getUsers() {
        List<AdminUserDto> users = auditLogService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success(200, "Users retrieved", users));
    }

    @Operation(summary = "Update user role", description = "Change a user's role (ADMIN/USER)")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Role updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    @PutMapping("/users/{id}/role")
    public ResponseEntity<ApiResponse<Void>> updateUserRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String role = body.get("role");
        if (role == null || (!role.equals("ROLE_ADMIN") && !role.equals("ROLE_USER"))) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest("Invalid role. Must be ROLE_ADMIN or ROLE_USER"));
        }
        auditLogService.updateUserRole(id, role);
        return ResponseEntity.ok(ApiResponse.success(200, "User role updated", null));
    }

    @Operation(summary = "Get admin stats", description = "Retrieve admin dashboard statistics")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Stats retrieved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required")
    })
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminStatsDto>> getStats() {
        AdminStatsDto stats = auditLogService.getAdminStats();
        return ResponseEntity.ok(ApiResponse.success(200, "Stats retrieved", stats));
    }
}
