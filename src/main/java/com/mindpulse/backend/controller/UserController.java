package com.mindpulse.backend.controller;

import com.mindpulse.backend.dto.ApiResponse;
import com.mindpulse.backend.dto.ChangePasswordDto;
import com.mindpulse.backend.dto.UpdateProfileDto;
import com.mindpulse.backend.dto.UserProfileDto;
import com.mindpulse.backend.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/users")
@Tag(name = "User Management", description = "User profile and account management")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;

    @Operation(summary = "Get current user profile")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile retrieved")})
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileDto>> getProfile() {
        Long userId = getCurrentUserId();
        UserProfileDto profile = userService.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(200, "Profile retrieved", profile));
    }

    @Operation(summary = "Update user profile")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile updated")})
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<Void>> updateProfile(@Valid @RequestBody UpdateProfileDto dto) {
        Long userId = getCurrentUserId();
        userService.updateProfile(userId, dto);
        return ResponseEntity.ok(ApiResponse.success(200, "Profile updated", null));
    }

    @Operation(summary = "Change password")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Password changed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid old password")
    })
    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordDto dto) {
        Long userId = getCurrentUserId();
        userService.changePassword(userId, dto);
        return ResponseEntity.ok(ApiResponse.success(200, "Password changed", null));
    }

    @Operation(summary = "Upload avatar")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Avatar uploaded")})
    @PostMapping("/avatar")
    public ResponseEntity<ApiResponse<String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long userId = getCurrentUserId();
        String uploadDir = System.getProperty("user.dir") + "/files/avatars";
        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();

        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        String filePath = uploadDir + "/" + fileName;
        try {
            file.transferTo(new File(filePath));
        } catch (Exception e) {
            log.error("Failed to upload avatar: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.badRequest("Failed to upload avatar"));
        }
        String avatarUrl = "/api/users/avatar/" + fileName;
        userService.updateAvatar(userId, avatarUrl);
        return ResponseEntity.ok(ApiResponse.success(200, "Avatar uploaded", avatarUrl));
    }

    @Operation(summary = "Get avatar image")
    @GetMapping(value = "/avatar/{filename}", produces = MediaType.ALL_VALUE)
    public ResponseEntity<org.springframework.core.io.Resource> getAvatar(@PathVariable String filename) {
        File file = new File(System.getProperty("user.dir") + "/files/avatars/" + filename);
        if (!file.exists()) return ResponseEntity.notFound().build();
        org.springframework.core.io.Resource resource = new org.springframework.core.io.FileSystemResource(file);
        return ResponseEntity.ok().body(resource);
    }

    private Long getCurrentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationServiceException("User is not authenticated");
        }
        String username = authentication.getName();
        var user = userService.findByUsername(username);
        if (user.isEmpty()) throw new AuthenticationServiceException("User not found");
        return user.get().getId();
    }
}
