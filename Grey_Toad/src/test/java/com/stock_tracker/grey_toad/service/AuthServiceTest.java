package com.stock_tracker.grey_toad.service;

import com.stock_tracker.grey_toad.data.UserRepository;
import com.stock_tracker.grey_toad.dto.LoginRequest;
import com.stock_tracker.grey_toad.dto.RegisterRequest;
import com.stock_tracker.grey_toad.entity.User;
import com.stock_tracker.grey_toad.exceptions.ConflictException;
import com.stock_tracker.grey_toad.exceptions.UnauthorizedException;
import com.stock_tracker.grey_toad.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerRejectsDuplicateEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(new User()));

        assertThrows(ConflictException.class, () -> authService.register(request));
    }

    @Test
    void registerHashesPasswordBeforeSave() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("neo");
        request.setEmail("neo@example.com");
        request.setPassword("plain");

        when(userRepository.findByEmail("neo@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("plain")).thenReturn("hashed");
        when(jwtService.generateToken("neo@example.com")).thenReturn("token");

        authService.register(request);

        verify(passwordEncoder).encode("plain");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void loginRejectsInvalidPassword() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("bad");

        User user = new User();
        user.setEmail("test@example.com");
        user.setPassword("hashed");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bad", "hashed")).thenReturn(false);

        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> authService.login(request));
        assertEquals("Invalid email or password", exception.getMessage());
    }
}
