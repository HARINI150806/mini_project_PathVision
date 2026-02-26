package com.pathvision.controller;

import com.pathvision.entity.User;
import com.pathvision.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dev")
public class DevController {

    private final UserRepository userRepository;

    public DevController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/enable-user")
    public ResponseEntity<?> enableUser(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "email is required"));
        }
        var opt = userRepository.findByEmail(email);
        if (opt.isEmpty()) return ResponseEntity.status(404).body(Map.of("message", "User not found"));
        User u = opt.get();
        u.setEnabled(true);
        userRepository.save(u);
        return ResponseEntity.ok(Map.of("message", "User enabled", "email", email));
    }
}
