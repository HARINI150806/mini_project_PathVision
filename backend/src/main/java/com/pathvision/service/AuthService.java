package com.pathvision.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pathvision.dto.AuthResponse;
import com.pathvision.dto.LoginRequest;
import com.pathvision.dto.RegisterRequest;
import com.pathvision.dto.VerifyRequest;
import com.pathvision.entity.Role;
import com.pathvision.entity.StudentProfile;
import com.pathvision.entity.User;
import com.pathvision.repository.StudentProfileRepository;
import com.pathvision.repository.UserRepository;
import com.pathvision.security.JwtService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
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

    @Value("${google.oauth.client-id:}")
    private String googleClientId;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JavaMailSender mailSender,
            JwtService jwtService,
            StudentProfileRepository studentProfileRepository
    ) {
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

        String verificationCode = String.valueOf(new Random().nextInt(900000) + 100000);

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

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(userRole)
                .verificationCode(verificationCode)
                .verificationCodeExpiresAt(LocalDateTime.now().plusMinutes(15))
                .enabled(false)
                .build();

        userRepository.save(user);
        sendVerificationEmail(user.getEmail(), verificationCode);

        return AuthResponse.builder()
                .token(null)
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

    public ResponseEntity<?> login(LoginRequest request) {
        if (!userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "This email is not registered"));
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
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

        try {
            if (user.getRole() == Role.STUDENT) {
                ensureStudentProfileExists(user);
            }
        } catch (Exception ignored) {
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

    public ResponseEntity<?> googleLogin(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Google token is required"));
        }

        final Map<String, Object> googleClaims;
        try {
            googleClaims = verifyGoogleIdToken(idToken);
        } catch (ResponseStatusException ex) {
            return ResponseEntity
                    .status(ex.getStatusCode())
                    .body(Map.of("message", ex.getReason() == null ? "Google authentication failed" : ex.getReason()));
        } catch (Exception ex) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Google authentication failed"));
        }

        String email = asString(googleClaims.get("email"));
        String fullName = asString(googleClaims.get("name"));
        if (email == null || email.isBlank()) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Google account email not available"));
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "This email is not registered. Please register first."));
        }

        if (!user.isEnabled()) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Account not verified. Please verify your email."));
        }

        if ((user.getFullName() == null || user.getFullName().isBlank()) && fullName != null && !fullName.isBlank()) {
            user.setFullName(fullName);
            user = userRepository.save(user);
        }

        try {
            if (user.getRole() == Role.STUDENT) {
                ensureStudentProfileExists(user);
            }
        } catch (Exception ignored) {
            System.err.println("Warning: failed to create student profile: " + ignored.getMessage());
        }

        final String token;
        try {
            token = jwtService.generateToken(user);
        } catch (Exception ex) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to generate authentication token", "error", ex.getMessage()));
        }

        return ResponseEntity.ok(
                AuthResponse.builder()
                        .token(token)
                        .message("Google login successful")
                        .role(user.getRole().name())
                        .name(user.getFullName())
                        .build()
        );
    }

    private void ensureStudentProfileExists(User user) {
        if (!studentProfileRepository.existsByUser(user)) {
            StudentProfile profile = new StudentProfile();
            profile.setUser(user);
            studentProfileRepository.save(profile);
        }
    }

    private Map<String, Object> verifyGoogleIdToken(String idToken) {
        try {
            String encodedToken = URLEncoder.encode(idToken, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://oauth2.googleapis.com/tokeninfo?id_token=" + encodedToken))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Google token");
            }

            Map<String, Object> claims =
                    objectMapper.readValue(response.body(), new TypeReference<HashMap<String, Object>>() {});

            String email = asString(claims.get("email"));
            String audience = asString(claims.get("aud"));
            String emailVerifiedRaw = asString(claims.get("email_verified"));
            boolean emailVerified = "true".equalsIgnoreCase(emailVerifiedRaw);

            if (email == null || email.isBlank()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google token has no email");
            }

            if (!emailVerified) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google email is not verified");
            }

            if (googleClientId != null && !googleClientId.isBlank() && !googleClientId.equals(audience)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google token audience mismatch");
            }

            return claims;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unable to verify Google token");
        }
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
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
            System.out.println("--------------------------------------------------");
            System.out.println("VERIFICATION CODE FOR " + to + ": " + code);
            System.out.println("--------------------------------------------------");
        } catch (Exception e) {
            System.out.println("--------------------------------------------------");
            System.out.println("VERIFICATION CODE FOR " + to + ": " + code);
            System.out.println("--------------------------------------------------");
        }
    }
}
