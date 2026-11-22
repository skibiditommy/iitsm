package com.iitsm.service;

import com.iitsm.config.JwtTokenProvider;
import com.iitsm.domain.enums.NotificationChannelType;
import com.iitsm.domain.enums.Role;
import com.iitsm.domain.exception.InvalidLoginException;
import com.iitsm.domain.exception.UserNotFoundException;
import com.iitsm.domain.factory.NotificationPayload;
import com.iitsm.domain.factory.UserFactory;
import com.iitsm.domain.model.User;
import com.iitsm.dto.request.ForgotPasswordRequest;
import com.iitsm.dto.request.LoginRequest;
import com.iitsm.dto.request.RegisterRequest;
import com.iitsm.dto.request.ResetPasswordRequest;
import com.iitsm.dto.response.AuthResponse;
import com.iitsm.infrastructure.adapter.EmailNotificationAdapter;
import com.iitsm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserFactory userFactory;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailNotificationAdapter emailAdapter;

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new InvalidLoginException("Invalid email or password"));

        if (user.isAccountLocked()) {
            throw new InvalidLoginException("Account is temporarily locked due to 5 consecutive failed login attempts. Locked until " + user.getLockedUntil());
        }

        if (!user.isActive()) {
            throw new InvalidLoginException("Account is deactivated");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            user.recordFailedLogin();
            userRepository.save(user);
            if (user.isAccountLocked()) {
                throw new InvalidLoginException("Account locked: 5 consecutive failed login attempts reached. Try again in 15 minutes.");
            }
            int remaining = 5 - user.getFailedLoginAttempts();
            throw new InvalidLoginException("Invalid email or password. " + remaining + " attempts remaining before temporary account lockout.");
        }

        user.resetFailedLogins();
        userRepository.save(user);

        String token = jwtTokenProvider.generateToken(user.getEmail(), user.getRole().name());
        log.info("User logged in: {}", user.getEmail());
        return new AuthResponse(token, user.getEmail(), user.getRole().name(), user.getFullName());
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new InvalidLoginException("Email already registered: " + request.email());
        }

        Role role = Role.valueOf(request.role().toUpperCase());
        String encodedPassword = passwordEncoder.encode(request.password());
        User user = userFactory.createUser(request.email(), encodedPassword,
                request.firstName(), request.lastName(), role, request.department(), request.phone());
        userRepository.save(user);

        String token = jwtTokenProvider.generateToken(user.getEmail(), user.getRole().name());
        log.info("User registered: {} as {}", user.getEmail(), role);
        return new AuthResponse(token, user.getEmail(), role.name(), user.getFullName());
    }

    @Transactional
    public Map<String, Object> forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UserNotFoundException(request.email()));

        String token = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        user.setResetToken(token, LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        // Send simulated email & persist to NotificationOutbox
        String subject = "Password Reset Verification Code - I-ITSM";
        String body = "Hello " + user.getFullName() + ",\n\n"
                + "A password reset was requested for your I-ITSM account (" + user.getRole() + ").\n"
                + "Your temporary reset verification code is: " + token + "\n\n"
                + "Enter this code on the reset password screen to complete the reset. This code is valid for 30 minutes.";

        emailAdapter.send(new NotificationPayload(
                NotificationChannelType.EMAIL,
                user.getEmail(),
                subject,
                body,
                "AUTH-RESET",
                "PASSWORD_RESET_REQUESTED"
        ));

        log.info("Password reset requested for {}. Code: {}", user.getEmail(), token);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Password reset verification email has been dispatched to " + user.getEmail());
        response.put("email", user.getEmail());
        response.put("resetCode", token);
        response.put("verificationCode", token);
        return response;
    }

    @Transactional
    public Map<String, String> resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UserNotFoundException(request.email()));

        if (!user.isResetTokenValid(request.resetToken())) {
            throw new InvalidLoginException("Invalid or expired password reset verification code");
        }

        user.updatePassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        emailAdapter.send(new NotificationPayload(
                NotificationChannelType.EMAIL,
                user.getEmail(),
                "Password Reset Confirmed - I-ITSM",
                "Hello " + user.getFullName() + ",\n\nYour password has been successfully updated. You may now log in with your new credentials.",
                "AUTH-RESET",
                "PASSWORD_RESET_COMPLETED"
        ));

        log.info("Password reset completed successfully for {}", user.getEmail());
        Map<String, String> response = new HashMap<>();
        response.put("message", "Password updated successfully! You can now log in with your new password.");
        return response;
    }
}
