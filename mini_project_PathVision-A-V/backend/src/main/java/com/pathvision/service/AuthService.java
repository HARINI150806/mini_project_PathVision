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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.pathvision.security.JwtService;
import com.pathvision.entity.StudentProfile;
import com.pathvision.repository.StudentProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JavaMailSender mailSender;
    private final JwtService jwtService;
    private final StudentProfileRepository studentProfileRepository;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JavaMailSender mailSender, JwtService jwtService, StudentProfileRepository studentProfileRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.mailSender = mailSender;
        this.jwtService = jwtService;
        this.studentProfileRepository = studentProfileRepository;
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

 public ResponseEntity<?> login(LoginRequest request) {

    if (!userRepository.existsByEmail(request.getEmail())) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "This email is not registered"));
    }

    try {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

    } catch (DisabledException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Account not verified. Please verify your email."));

    } catch (BadCredentialsException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Incorrect password. Please try again."));
    }


    User user = userRepository.findByEmail(request.getEmail()).get();

    String token;
    try {
        token = jwtService.generateToken(user);
    } catch (Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Failed to generate authentication token", "error", ex.getMessage()));
    }

    // Create an empty student profile on first successful login for STUDENT role
    try {
        if (user.getRole() == Role.STUDENT) {
            boolean exists = studentProfileRepository.existsByUser(user);
            if (!exists) {
                StudentProfile profile = new StudentProfile();
                profile.setUser(user);
                // leave other fields null/empty for the user to populate later
                studentProfileRepository.save(profile);
            }
        }
    } catch (Exception ignored) {
        // don't fail login if profile creation has issues; log if necessary
        System.err.println("Warning: failed to create student profile: " + ignored.getMessage());
    }

    return ResponseEntity.ok(
            AuthResponse.builder()
                    .token(token)
                    .message("Login successful")
                    .role(user.getRole().name())
                    .name(user.getFullName())
                    .build()
    );
}



}
