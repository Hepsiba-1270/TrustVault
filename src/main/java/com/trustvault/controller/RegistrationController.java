
package com.trustvault.controller;

import com.trustvault.entity.AppUser;
import com.trustvault.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RegistrationController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/signup")
    public String showSignupPage() {
        return "signup";
    }

    @PostMapping("/signup")
    public String registerUser(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            @RequestParam(defaultValue = "VIEWER") String role,
            Model model,
            RedirectAttributes redirectAttributes) {

        String cleanUsername = username.trim();

        // Validate username.
        if (cleanUsername.isBlank()
                || cleanUsername.length() < 3
                || cleanUsername.length() > 50) {
            model.addAttribute(
                    "error",
                    "Username must contain 3 to 50 characters.");
            return "signup";
        }

        // Require at least 8 characters and all four character types.
        if (password.length() < 8
                || !password.matches(".*[A-Z].*")
                || !password.matches(".*[a-z].*")
                || !password.matches(".*[0-9].*")
                || !password.matches(".*[^A-Za-z0-9].*")) {
            model.addAttribute(
                    "error",
                    "Password must have at least 8 characters, "
                    + "including uppercase, lowercase, a number, "
                    + "and a special character.");
            return "signup";
        }

        // Check that both password fields match.
        if (!password.equals(confirmPassword)) {
            model.addAttribute(
                    "error",
                    "Passwords do not match.");
            return "signup";
        }

        // Check for duplicate usernames.
        if (userRepository.existsByUsername(cleanUsername)) {
            model.addAttribute(
                    "error",
                    "Username already exists. Please choose another.");
            return "signup";
        }

        // Public registration must never create privileged accounts.
        if (!"VIEWER".equalsIgnoreCase(role.trim())) {
            model.addAttribute(
                    "error",
                    "Public signup is available for Viewer accounts only.");
            return "signup";
        }

        // Create the account with an encoded password.
        AppUser user = new AppUser();
        user.setUsername(cleanUsername);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("VIEWER");

        userRepository.save(user);

        // Redirect to login after successful registration.
        redirectAttributes.addFlashAttribute(
                "success",
                "Account created successfully. You can now sign in.");

        return "redirect:/login";
    }
}
