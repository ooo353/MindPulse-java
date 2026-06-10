package com.mindpulse.backend.service;

import com.mindpulse.backend.dto.UserDto;
import com.mindpulse.backend.entity.User;
import com.mindpulse.backend.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements IUserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    @CacheEvict(value = "users", key = "#result.username")
    public User registerUser(UserDto userDto) {
        log.info("Attempting to register user: {}", userDto.username());
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
        user.setRole("ROLE_USER");
        user.setCreatedAt(java.time.LocalDateTime.now());
        user.setUpdatedAt(java.time.LocalDateTime.now());
        userMapper.insertUser(user);
        log.info("User registered successfully: {}", user.getUsername());
        return user;
    }

    @Override
    @Cacheable(value = "users", key = "#username")
    public Optional<User> findByUsername(String username) {
        User user = userMapper.findByUsername(username);
        return user != null ? Optional.of(user) : Optional.empty();
    }

    @Override
    @Cacheable(value = "users", key = "#identifier")
    public Optional<User> findByUsernameOrEmail(String identifier) {
        User user = null;

        if (identifier.contains("@")) {
            user = userMapper.findByEmail(identifier);
        } else {
            user = userMapper.findByUsername(identifier);
        }

        return user != null ? Optional.of(user) : Optional.empty();
    }

    @Override
    @Cacheable(value = "users", key = "#id")
    public Optional<User> findById(Long id) {
        User user = userMapper.findById(id);
        return user != null ? Optional.of(user) : Optional.empty();
    }
}
