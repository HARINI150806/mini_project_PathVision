package com.pathvision.controller;

import com.pathvision.dto.AdminUserResponse;
import com.pathvision.dto.AdminUserUpdateRequest;
import com.pathvision.entity.User;
import com.pathvision.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserRepository userRepository;

    public AdminUserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<AdminUserResponse> listUsers() {
        return userRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(User::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(AdminUserResponse::fromEntity)
                .toList();
    }

    @PutMapping("/{id}")
    public AdminUserResponse updateUser(@PathVariable Long id,
                                        @RequestBody AdminUserUpdateRequest request,
                                        Authentication authentication) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String currentAdminEmail = authentication == null ? null : authentication.getName();
        boolean isSelf = currentAdminEmail != null && currentAdminEmail.equalsIgnoreCase(user.getEmail());

        if (request.getRole() != null && !request.getRole().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role change is disabled");
        }

        if (request.getEnabled() != null) {
            if (isSelf && !request.getEnabled()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot disable your own account");
            }
            user.setEnabled(request.getEnabled());
        }

        userRepository.save(user);
        return AdminUserResponse.fromEntity(user);
    }

    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable Long id, Authentication authentication) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String currentAdminEmail = authentication == null ? null : authentication.getName();
        if (currentAdminEmail != null && currentAdminEmail.equalsIgnoreCase(user.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot delete your own account");
        }

        userRepository.delete(user);
    }
}
