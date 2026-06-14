package com.mindpulse.backend.service;

import com.mindpulse.backend.dto.ChangePasswordDto;
import com.mindpulse.backend.dto.UpdateProfileDto;
import com.mindpulse.backend.dto.UserDto;
import com.mindpulse.backend.dto.UserProfileDto;
import com.mindpulse.backend.entity.User;

import java.util.Optional;

public interface IUserService {
    User registerUser(UserDto userDto);
    Optional<User> findByUsername(String username);
    Optional<User> findByUsernameOrEmail(String identifier);
    Optional<User> findById(Long id);
    UserProfileDto getProfile(Long userId);
    void updateProfile(Long userId, UpdateProfileDto dto);
    void changePassword(Long userId, ChangePasswordDto dto);
    void updateAvatar(Long userId, String avatarUrl);
}
