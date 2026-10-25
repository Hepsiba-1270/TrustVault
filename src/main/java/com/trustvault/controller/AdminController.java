
package com.trustvault.controller;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.trustvault.entity.AppUser;
import com.trustvault.entity.AuditLog;
import com.trustvault.repository.AuditLogRepository;
import com.trustvault.repository.DocumentPermissionRepository;
import com.trustvault.repository.DocumentRepository;
import com.trustvault.repository.UserRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final DocumentPermissionRepository permissionRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminController(
            UserRepository userRepository,
            DocumentRepository documentRepository,
            DocumentPermissionRepository permissionRepository,
            AuditLogRepository auditLogRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
        this.permissionRepository = permissionRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Return dashboard statistics.
    @GetMapping("/stats")
    public Map<String, Long> getStatistics() {
        Map<String, Long> stats = new LinkedHashMap<>();

        stats.put("totalUsers", userRepository.count());
        stats.put("totalDocuments", documentRepository.count());
        stats.put("totalPermissions", permissionRepository.count());
        stats.put("totalAuditLogs", auditLogRepository.count());

        return stats;
    }

    // Return the five most recent audit logs.
    @GetMapping("/recent-logs")
    public List<AuditLog> getRecentLogs() {
        return auditLogRepository
                .findAllByOrderByTimestampDesc()
                .stream()
                .limit(5)
                .toList();
    }

    // Return user details without exposing passwords or recovery data.
    @GetMapping("/users")
    public List<Map<String, Object>> getUsers() {
        return userRepository.findAll()
                .stream()
                .map(user -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("id", user.getId());
                    result.put("username", user.getUsername());
                    result.put("role", normalizeRole(user.getRole()));
                    return result;
                })
                .toList();
    }

    // Create an administrator-managed account.
    @PostMapping("/users")
    public ResponseEntity<?> createUser(
            @Valid @RequestBody CreateUserRequest request) {

        String username = request.username().trim();

        if (userRepository.existsByUsername(username)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Username already exists."));
        }

        String role = normalizeRole(request.role());

        if (!Set.of("OWNER", "ADMIN").contains(role)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message",
                            "Admin can create OWNER or ADMIN accounts only."));
        }

        AppUser user = new AppUser(
                username,
                passwordEncoder.encode(request.password()),
                role
        );

        AppUser savedUser = userRepository.save(user);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", savedUser.getId());
        result.put("username", savedUser.getUsername());
        result.put("role", normalizeRole(savedUser.getRole()));

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    // Change an existing user's role.
    @PutMapping("/users/{id}/role")
    public ResponseEntity<?> changeUserRole(
            @PathVariable Long id,
            @Valid @RequestBody ChangeRoleRequest request) {

        AppUser user = userRepository.findById(id).orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "User not found."));
        }

        String newRole = normalizeRole(request.role());

        if (!Set.of("VIEWER", "OWNER", "ADMIN").contains(newRole)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Invalid role."));
        }

        String currentRole = normalizeRole(user.getRole());

        // Prevent removing the final administrator.
        if ("ADMIN".equals(currentRole) && !"ADMIN".equals(newRole)) {
            long adminCount = userRepository.findAll()
                    .stream()
                    .filter(existing ->
                            "ADMIN".equals(normalizeRole(existing.getRole())))
                    .count();

            if (adminCount <= 1) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message",
                                "The last administrator cannot be demoted."));
            }
        }

        user.setRole(newRole);
        AppUser savedUser = userRepository.save(user);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", savedUser.getId());
        result.put("username", savedUser.getUsername());
        result.put("role", normalizeRole(savedUser.getRole()));

        return ResponseEntity.ok(result);
    }

    // Generate a temporary password for an existing user.
    @PostMapping("/users/{username}/reset-password")
    @Transactional
    public ResponseEntity<?> resetUserPassword(
            @PathVariable String username) {

        AppUser user = userRepository.findByUsername(username)
                .orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "User not found."));
        }

        byte[] bytes = new byte[18];
        new SecureRandom().nextBytes(bytes);

        String temporaryPassword = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);

        user.setPassword(passwordEncoder.encode(temporaryPassword));

        // These methods must exist in AppUser.java.
        user.setMustChangePassword(true);
        user.setRecoveryCodeHash(null);

        userRepository.save(user);

        Map<String, String> result = new LinkedHashMap<>();
        result.put("message",
                "Temporary password created. Share it securely.");
        result.put("temporaryPassword", temporaryPassword);

        return ResponseEntity.ok(result);
    }

    // Normalize role names consistently.
    private String normalizeRole(String role) {
        if (role == null) {
            return "";
        }

        String normalized = role.trim().toUpperCase(Locale.ROOT);

        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring(5);
        }

        return normalized;
    }

    public record CreateUserRequest(
            @NotBlank
            @Size(min = 3, max = 50)
            @Pattern(regexp = "^[A-Za-z0-9_.-]+$")
            String username,

            @NotBlank
            @Size(min = 8, max = 72)
            @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S+$"
            )
            String password,

            @NotBlank
            String role
    ) {}

    public record ChangeRoleRequest(
            @NotBlank
            String role
    ) {}
}
