package com.pathvision.service;

import com.pathvision.dto.AuthResponse;
import com.pathvision.dto.LoginRequest;
import com.pathvision.dto.RegisterRequest;
import com.pathvision.dto.VerifyRequest; 
import com.pathvision.entity.Role;
import com.pathvision.entity.User;
import com.pathvision.repository.UserRepository;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.mailSender = mailSender;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already in use");
        }

        // Generate verification code
        String verificationCode = String.valueOf(new Random().nextInt(900000) + 100000); // 6-digit code

        Role userRole;
        if (request.getRole() != null) {
            switch (request.getRole().toUpperCase()) {
                case "ADMIN":
                    userRole = Role.ADMIN;
                    break;
                case "PROFESSIONAL":
                case "PROFESSIONAL LEARNER":
                    userRole = Role.PROFESSIONAL;
                    break;
                default:
                    userRole = Role.STUDENT;
            }
        } else {
            userRole = Role.STUDENT;
        }

        var user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(userRole)
                .verificationCode(verificationCode)
                .verificationCodeExpiresAt(LocalDateTime.now().plusMinutes(15))
                .enabled(false)
                .build();
        
        userRepository.save(user); // Save first to persist
        
        // Send Verification Email
        sendVerificationEmail(user.getEmail(), verificationCode);

        return AuthResponse.builder()
                .token(null) // No token yet
                .message("Verification code sent to email: " + user.getEmail())
                .role(null)
                .name(null)
                .build();
    }

    public void verify(VerifyRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.isEnabled()) {
            throw new RuntimeException("User already verified");
        }

        if (user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Verification code expired");
        }

        if (user.getVerificationCode().equals(request.getVerificationCode())) {
            user.setEnabled(true);
            user.setVerificationCode(null);
            user.setVerificationCodeExpiresAt(null);
            userRepository.save(user);
        } else {
            throw new RuntimeException("Invalid verification code");
        }
    }

    private void sendVerificationEmail(String to, String code) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            helper.setText("Your verification code is: " + code, true); 
            helper.setTo(to);
            helper.setSubject("Verify your email");
            helper.setFrom(fromEmail);
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            System.err.println("Failed to send email: " + e.getMessage()); 
            // Fallback for demo: Print to console so user can still proceed
            System.out.println("--------------------------------------------------");
            System.out.println("VERIFICATION CODE FOR " + to + ": " + code);
            System.out.println("--------------------------------------------------");
        } catch (Exception e) {
             System.out.println("--------------------------------------------------");
             System.out.println("VERIFICATION CODE FOR " + to + ": " + code);
             System.out.println("--------------------------------------------------");
        }
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            var user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!user.isEnabled()) {
                throw new RuntimeException("Account not verified. Please verify your email.");
            }

            // In a real app, use JWT utils
            String token = "dummy-jwt-token-for-" + user.getId();

            return AuthResponse.builder()
                    .token(token)
                    .message("Login successful")
                    .role(user.getRole().name())
                    .name(user.getFullName())
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Invalid email or password");
        }
    }
}
