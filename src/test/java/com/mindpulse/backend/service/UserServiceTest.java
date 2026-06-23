package com.mindpulse.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindpulse.backend.dto.UserDto;
import com.mindpulse.backend.dto.ChangePasswordDto;
import com.mindpulse.backend.entity.User;
import com.mindpulse.backend.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private UserService userService;

    private User sampleUser;
    private UserDto sampleUserDto;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setUsername("testuser");
        sampleUser.setEmail("test@example.com");
        sampleUser.setPassword("$2a$10$encodedPassword");
        sampleUser.setRole("ROLE_USER");
        sampleUser.setCreatedAt(LocalDateTime.now());
        sampleUser.setUpdatedAt(LocalDateTime.now());

        sampleUserDto = new UserDto("testuser", "password123", "test@example.com");
    }

    // --- registerUser ---

    @Test
    @DisplayName("should_RegisterUser_When_ValidDto")
    void should_RegisterUser_When_ValidDto() {
        // Arrange
        when(userMapper.existsByUsername("testuser")).thenReturn(false);
        when(userMapper.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encodedPassword");

        // Act
        User result = userService.registerUser(sampleUserDto);

        // Assert
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("$2a$10$encodedPassword", result.getPassword());
        assertEquals("ROLE_USER", result.getRole());
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getUpdatedAt());
        verify(userMapper, times(1)).insertUser(any(User.class));
    }

    @Test
    @DisplayName("should_ThrowException_When_UsernameAlreadyExists")
    void should_ThrowException_When_UsernameAlreadyExists() {
        // Arrange
        when(userMapper.existsByUsername("testuser")).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.registerUser(sampleUserDto));
        assertEquals("Username already exists", exception.getMessage());
        verify(userMapper, never()).insertUser(any(User.class));
    }

    @Test
    @DisplayName("should_ThrowException_When_EmailAlreadyExists")
    void should_ThrowException_When_EmailAlreadyExists() {
        // Arrange
        when(userMapper.existsByUsername("testuser")).thenReturn(false);
        when(userMapper.existsByEmail("test@example.com")).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.registerUser(sampleUserDto));
        assertEquals("Email already exists", exception.getMessage());
        verify(userMapper, never()).insertUser(any(User.class));
    }

    // --- findByUsername ---

    @Test
    @DisplayName("should_ReturnUser_When_UsernameExists")
    void should_ReturnUser_When_UsernameExists() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(userMapper.findByUsername("testuser")).thenReturn(sampleUser);

        // Act
        Optional<User> result = userService.findByUsername("testuser");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
        assertEquals("test@example.com", result.get().getEmail());
        verify(userMapper, times(1)).findByUsername("testuser");
    }

    @Test
    @DisplayName("should_ReturnEmpty_When_UsernameNotFound")
    void should_ReturnEmpty_When_UsernameNotFound() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(userMapper.findByUsername("nonexistent")).thenReturn(null);

        // Act
        Optional<User> result = userService.findByUsername("nonexistent");

        // Assert
        assertTrue(result.isEmpty());
    }

    // --- findByUsernameOrEmail ---

    @Test
    @DisplayName("should_FindByEmail_When_IdentifierContainsAt")
    void should_FindByEmail_When_IdentifierContainsAt() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(userMapper.findByEmail("test@example.com")).thenReturn(sampleUser);

        // Act
        Optional<User> result = userService.findByUsernameOrEmail("test@example.com");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
        verify(userMapper, times(1)).findByEmail("test@example.com");
        verify(userMapper, never()).findByUsername(anyString());
    }

    @Test
    @DisplayName("should_FindByUsername_When_IdentifierHasNoAt")
    void should_FindByUsername_When_IdentifierHasNoAt() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(userMapper.findByUsername("testuser")).thenReturn(sampleUser);

        // Act
        Optional<User> result = userService.findByUsernameOrEmail("testuser");

        // Assert
        assertTrue(result.isPresent());
        verify(userMapper, times(1)).findByUsername("testuser");
        verify(userMapper, never()).findByEmail(anyString());
    }

    // --- findById ---

    @Test
    @DisplayName("should_ReturnUser_When_IdExists")
    void should_ReturnUser_When_IdExists() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(userMapper.findById(1L)).thenReturn(sampleUser);

        // Act
        Optional<User> result = userService.findById(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        verify(userMapper, times(1)).findById(1L);
    }

    @Test
    @DisplayName("should_ReturnEmpty_When_IdNotFound")
    void should_ReturnEmpty_When_IdNotFound() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(userMapper.findById(999L)).thenReturn(null);

        // Act
        Optional<User> result = userService.findById(999L);

        // Assert
        assertTrue(result.isEmpty());
    }

    // --- changePassword ---

    @Test
    @DisplayName("should_ChangePassword_When_OldPasswordMatches")
    void should_ChangePassword_When_OldPasswordMatches() {
        // Arrange
        ChangePasswordDto dto = new ChangePasswordDto("oldPassword", "newPassword");
        when(userMapper.findById(1L)).thenReturn(sampleUser);
        when(passwordEncoder.matches("oldPassword", "$2a$10$encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("$2a$10$newEncodedPassword");

        // Act
        userService.changePassword(1L, dto);

        // Assert
        verify(userMapper, times(1)).updatePassword(1L, "$2a$10$newEncodedPassword");
    }

    @Test
    @DisplayName("should_ThrowException_When_OldPasswordDoesNotMatch")
    void should_ThrowException_When_OldPasswordDoesNotMatch() {
        // Arrange
        ChangePasswordDto dto = new ChangePasswordDto("wrongPassword", "newPassword");
        when(userMapper.findById(1L)).thenReturn(sampleUser);
        when(passwordEncoder.matches("wrongPassword", "$2a$10$encodedPassword")).thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.changePassword(1L, dto));
        assertEquals("Current password is incorrect", exception.getMessage());
        verify(userMapper, never()).updatePassword(anyLong(), anyString());
    }

    @Test
    @DisplayName("should_ThrowException_When_UserNotFoundOnPasswordChange")
    void should_ThrowException_When_UserNotFoundOnPasswordChange() {
        // Arrange
        ChangePasswordDto dto = new ChangePasswordDto("oldPassword", "newPassword");
        when(userMapper.findById(999L)).thenReturn(null);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.changePassword(999L, dto));
        assertEquals("User not found", exception.getMessage());
    }
}
