
package com.trustvault.service;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.Base64;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trustvault.entity.AppUser;
import com.trustvault.repository.UserRepository;

@Service
public class PasswordRecoveryService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    public PasswordRecoveryService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Normalize recovery answers before hashing or verification.
    private String normalizeAnswer(String answer) {
        if (answer == null || answer.isBlank()) {
            return "";
        }

        return Normalizer.normalize(
                answer.trim(),
                Normalizer.Form.NFKC
        ).toLowerCase(Locale.ROOT);
    }

    // Save the selected recovery question and hashed answer.
    @Transactional
    public void saveRecoveryQuestion(
            AppUser user, String question, String answer) {

        if (user == null) {
            throw new IllegalArgumentException("User is required.");
        }

        if (question == null || question.isBlank()
                || answer == null || answer.isBlank()) {
            throw new IllegalArgumentException(
                    "Select a recovery question and enter its answer.");
        }

        user.setRecoveryQuestion(question.trim());
        user.setRecoveryAnswerHash(
                passwordEncoder.encode(normalizeAnswer(answer)));

        userRepository.save(user);
    }

    // Generate a recovery code for an authenticated user.
    @Transactional
    public String generateRecoveryCode(String username) {

        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found."));

        byte[] bytes = new byte[24];
        random.nextBytes(bytes);

        String code = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);

        user.setRecoveryCodeHash(passwordEncoder.encode(code));
        userRepository.save(user);

        // Display or deliver this code securely only once.
        return code;
    }

    // Reset the password using the one-time recovery code.
    @Transactional
    public boolean resetWithRecoveryCode(
            String username, String code, String newPassword) {

        if (username == null || username.isBlank()
                || code == null || code.isBlank()) {
            return false;
        }

        AppUser user = userRepository.findByUsername(username)
                .orElse(null);

        if (user == null
                || user.getRecoveryCodeHash() == null
                || !passwordEncoder.matches(
                        code, user.getRecoveryCodeHash())) {
            return false;
        }

        changePassword(user, newPassword);

        // Invalidate the recovery code after successful use.
        user.setRecoveryCodeHash(null);
        userRepository.save(user);

        return true;
    }

    // Reset the password after verifying the recovery answer.
    @Transactional
    public boolean resetWithQuestion(
            String username, String answer, String newPassword) {

        if (username == null || username.isBlank()
                || answer == null || answer.isBlank()) {
            return false;
        }

        AppUser user = userRepository.findByUsername(username)
                .orElse(null);

        if (user == null
                || user.getRecoveryAnswerHash() == null
                || !passwordEncoder.matches(
                        normalizeAnswer(answer),
                        user.getRecoveryAnswerHash())) {
            return false;
        }

        changePassword(user, newPassword);

        // Invalidate any previously generated recovery code.
        user.setRecoveryCodeHash(null);
        userRepository.save(user);

        return true;
    }

    // Validate and store the new password.
    private void changePassword(AppUser user, String newPassword) {

        if (newPassword == null
                || newPassword.length() < 8
                || newPassword.length() > 72) {
            throw new IllegalArgumentException(
                    "Password must be between 8 and 72 characters.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
    }
}
