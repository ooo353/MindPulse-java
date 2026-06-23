package com.mindpulse.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindpulse.backend.dto.ChangePasswordDto;
import com.mindpulse.backend.dto.UpdateProfileDto;
import com.mindpulse.backend.dto.UserDto;
import com.mindpulse.backend.dto.UserProfileDto;
import com.mindpulse.backend.entity.User;
import com.mindpulse.backend.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements IUserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CACHE_PREFIX = "users:";
    private static final long CACHE_TTL_MINUTES = 30;

    // --- Manual cache helpers ---

    private <T> T getFromCache(String key, TypeReference<T> typeRef) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                return objectMapper.readValue(json, typeRef);
            }
        } catch (Exception e) {
            log.warn("Redis cache read failed for key={}: {}", key, e.getMessage());
        }
        return null;
    }

    private void putToCache(String key, Object value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Redis cache write failed for key={}: {}", key, e.getMessage());
        }
    }

    private void evictCache(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Redis cache evict failed for key={}: {}", key, e.getMessage());
        }
    }

    // --- Service methods ---

    @Override
    @Transactional
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

        evictCache(CACHE_PREFIX + user.getUsername());
        return user;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        String key = CACHE_PREFIX + username;
        User cached = getFromCache(key, new TypeReference<User>() {});
        if (cached != null) {
            return Optional.of(cached);
        }
        User user = userMapper.findByUsername(username);
        if (user != null) {
            putToCache(key, user);
            return Optional.of(user);
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findByUsernameOrEmail(String identifier) {
        String key = CACHE_PREFIX + identifier;
        User cached = getFromCache(key, new TypeReference<User>() {});
        if (cached != null) {
            return Optional.of(cached);
        }

        User user = null;
        if (identifier.contains("@")) {
            user = userMapper.findByEmail(identifier);
        } else {
            user = userMapper.findByUsername(identifier);
        }

        if (user != null) {
            putToCache(key, user);
            return Optional.of(user);
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findById(Long id) {
        String key = CACHE_PREFIX + "id_" + id;
        User cached = getFromCache(key, new TypeReference<User>() {});
        if (cached != null) {
            return Optional.of(cached);
        }
        User user = userMapper.findById(id);
        if (user != null) {
            putToCache(key, user);
            return Optional.of(user);
        }
        return Optional.empty();
    }

    @Override
    public UserProfileDto getProfile(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        return new UserProfileDto(
            user.getId(),
            user.getUsername(),
            user.getNickname(),
            user.getEmail(),
            user.getAvatar(),
            user.getRole(),
            user.getCreatedAt()
        );
    }

    @Override
    @Transactional
    public void updateProfile(Long userId, UpdateProfileDto dto) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        // Check email uniqueness if email is being changed
        if (dto.email() != null && !dto.email().equals(user.getEmail())) {
            if (userMapper.existsByEmail(dto.email())) {
                throw new RuntimeException("Email is already in use");
            }
        }
        userMapper.updateProfile(userId, dto.nickname(), dto.email());
        evictUserCache(user);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordDto dto) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        if (!passwordEncoder.matches(dto.oldPassword(), user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }
        String encodedPassword = passwordEncoder.encode(dto.newPassword());
        userMapper.updatePassword(userId, encodedPassword);
        evictUserCache(user);
    }

    @Override
    @Transactional
    public void updateAvatar(Long userId, String avatarUrl) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        userMapper.updateAvatar(userId, avatarUrl);
        evictUserCache(user);
    }

    private void evictUserCache(User user) {
        evictCache(CACHE_PREFIX + user.getUsername());
        evictCache(CACHE_PREFIX + "id_" + user.getId());
        if (user.getEmail() != null) {
            evictCache(CACHE_PREFIX + user.getEmail());
        }
    }
}
