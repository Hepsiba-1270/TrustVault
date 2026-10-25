
package com.trustvault.controller;

import java.security.Principal;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.trustvault.entity.AppUser;
import com.trustvault.repository.UserRepository;
import com.trustvault.service.PasswordRecoveryService;

@RestController
@RequestMapping("/api/account")
public class PasswordRecoveryController {

    private final UserRepository userRepository;
    private final PasswordRecoveryService recoveryService;

    public PasswordRecoveryController(
            UserRepository userRepository,
            PasswordRecoveryService recoveryService) {
        this.userRepository = userRepository;
        this.recoveryService = recoveryService;
    }

    @PostMapping("/recovery-question")
    public ResponseEntity<?> saveQuestion(
            Principal principal,
            @RequestBody QuestionRequest request) {

        AppUser user = userRepository.findByUsername(principal.getName())
                .orElseThrow();

        recoveryService.saveRecoveryQuestion(
                user, request.question(), request.answer());

        return ResponseEntity.ok(Map.of(
                "message", "Recovery question saved."));
    }

    @PostMapping("/recovery-code")
    public ResponseEntity<?> generateCode(Principal principal) {
        String code = recoveryService.generateRecoveryCode(
                principal.getName());

        return ResponseEntity.ok(Map.of(
                "message", "Save your recovery code now. It is shown once.",
                "recoveryCode", code));
    }

    @PostMapping("/reset-by-question")
    public ResponseEntity<?> resetByQuestion(
            @RequestBody QuestionResetRequest request) {

        if (request.username() == null || request.answer() == null
                || request.newPassword() == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Enter all required fields."));
        }

        boolean success = recoveryService.resetWithQuestion(
                request.username().trim(),
                request.answer(),
                request.newPassword());

        if (!success) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Invalid username or recovery answer."));
        }

        return ResponseEntity.ok(Map.of(
                "message", "Password reset successful. Please log in."));
    }

    @PostMapping("/reset-by-code")
    public ResponseEntity<?> resetByCode(
            @RequestBody CodeResetRequest request) {

        if (request.username() == null || request.code() == null
                || request.newPassword() == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Enter all required fields."));
        }

        boolean success = recoveryService.resetWithRecoveryCode(
                request.username().trim(),
                request.code().trim(),
                request.newPassword());

        if (!success) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Invalid username or recovery code."));
        }

        return ResponseEntity.ok(Map.of(
                "message", "Password reset successful. Please log in."));
    }

    public record QuestionRequest(String question, String answer) {}
    public record QuestionResetRequest(
            String username, String answer, String newPassword) {}
    public record CodeResetRequest(
            String username, String code, String newPassword) {}
}
