package com.mindpulse.backend.service;

import com.mindpulse.backend.entity.User;
import com.mindpulse.backend.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @CacheEvict(value = "users", key = "#result.username")
    public User registerUser(com.mindpulse.backend.dto.UserDto userDto) {
        if (userMapper.existsByUsername(userDto.username())) {
            throw new RuntimeException("Username already exists");
        }
        if (userMapper.existsByEmail(userDto.email())) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setUsername(userDto.username());
        user.setEmail(userDto.email());
        user.setPassword(passwordEncoder.encode(userDto.password()));
        user.setRole("ROLE_USER"); // 设置默认角色
        user.setCreatedAt(java.time.LocalDateTime.now());
        user.setUpdatedAt(java.time.LocalDateTime.now());
        userMapper.insertUser(user);
        return user;
    }

    @Cacheable(value = "users", key = "#username")
    public Optional<User> findByUsername(String username) {
        User user = userMapper.findByUsername(username);
        return user != null ? Optional.of(user) : Optional.empty();
    }

    @Cacheable(value = "users", key = "#identifier")
    public Optional<User> findByUsernameOrEmail(String identifier) {
        User user = null;
        
        // Check if identifier is an email
        if (identifier.contains("@")) {
            user = userMapper.findByEmail(identifier);
        } else {
            user = userMapper.findByUsername(identifier);
        }
        
        return user != null ? Optional.of(user) : Optional.empty();
    }

    @Cacheable(value = "users", key = "#id")
    public Optional<User> findById(Long id) {
        User user = userMapper.findById(id);
        return user != null ? Optional.of(user) : Optional.empty();
    }
}