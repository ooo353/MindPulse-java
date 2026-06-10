package com.mindpulse.backend.service;

import com.mindpulse.backend.dto.UserDto;
import com.mindpulse.backend.entity.User;

import java.util.Optional;

public interface IUserService {
    User registerUser(UserDto userDto);
    Optional<User> findByUsername(String username);
    Optional<User> findByUsernameOrEmail(String identifier);
    Optional<User> findById(Long id);
}
