package com.example.ci.unit;

import com.example.ci.dto.UserDto;
import com.example.ci.model.User;
import com.example.ci.repository.UserRepository;
import com.example.ci.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Testing Tier:
 * Validates business logic in isolation using JUnit 5 and Mockito mocks.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("Unit Test: Should create user successfully when email is unique")
    void testCreateUser_Success() {
        UserDto inputDto = new UserDto(null, "John Doe", "john.doe@example.com", "ENGINEER", true);
        User savedEntity = new User(1L, "John Doe", "john.doe@example.com", "ENGINEER", true);

        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(savedEntity);

        UserDto result = userService.createUser(inputDto);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("john.doe@example.com");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Unit Test: Should throw DuplicateEmailException when email already exists")
    void testCreateUser_DuplicateEmail_ThrowsException() {
        UserDto inputDto = new UserDto(null, "Jane Doe", "jane.doe@example.com", "MANAGER", true);

        when(userRepository.existsByEmail("jane.doe@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(inputDto))
                .isInstanceOf(UserService.DuplicateEmailException.class)
                .hasMessageContaining("already exists");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Unit Test: Should retrieve user by ID successfully")
    void testGetUserById_Success() {
        User user = new User(10L, "Alice Smith", "alice@example.com", "ADMIN", true);
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        UserDto result = userService.getUserById(10L);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getName()).isEqualTo("Alice Smith");
    }

    @Test
    @DisplayName("Unit Test: Should throw UserNotFoundException when user does not exist")
    void testGetUserById_NotFound_ThrowsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(UserService.UserNotFoundException.class)
                .hasMessageContaining("User not found with id: 99");
    }

    @Test
    @DisplayName("Unit Test: Should delete user successfully when user exists")
    void testDeleteUser_Success() {
        when(userRepository.existsById(5L)).thenReturn(true);
        doNothing().when(userRepository).deleteById(5L);

        userService.deleteUser(5L);

        verify(userRepository, times(1)).deleteById(5L);
    }
}
