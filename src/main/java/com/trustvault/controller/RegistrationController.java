
package com.trustvault.controller;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.trustvault.entity.AppUser;
import com.trustvault.repository.UserRepository;
import com.trustvault.service.PasswordRecoveryService;

@Controller
public class RegistrationController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordRecoveryService recoveryService;

    public RegistrationController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            PasswordRecoveryService recoveryService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.recoveryService = recoveryService;
    }

    // Display the signup page.
    @GetMapping("/signup")
    public String showSignupPage() {
        return "signup";
    }

    // Register a new viewer account.
    @PostMapping("/signup")
    public String register(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            @RequestParam(defaultValue = "VIEWER") String role,
            @RequestParam String recoveryQuestion,
            @RequestParam String recoveryAnswer,
            Model model,
            RedirectAttributes redirectAttributes) {

        String cleanUsername = username.trim();

        // Validate the username.
        if (cleanUsername.isEmpty() || cleanUsername.length() > 50) {
            model.addAttribute("error",
                    "Username must contain 1 to 50 characters.");
            return "signup";
        }

        // Validate the password.
        if (password == null || password.length() < 8
                || password.length() > 72) {
            model.addAttribute("error",
                    "Password must be between 8 and 72 characters.");
            return "signup";
        }

        // Check whether both passwords match.
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error",
                    "Password and confirm password do not match.");
            return "signup";
        }

        // Prevent duplicate usernames.
        if (userRepository.findByUsername(cleanUsername).isPresent()) {
            model.addAttribute("error",
                    "Username already exists. Please choose another.");
            return "signup";
        }

        // Public signup must not create administrator accounts.
        if (!"VIEWER".equalsIgnoreCase(role.trim())) {
            model.addAttribute("error",
                    "Only Viewer accounts can be created through signup.");
            return "signup";
        }

        // Validate the recovery details.
        if (recoveryQuestion == null || recoveryQuestion.isBlank()
                || recoveryAnswer == null || recoveryAnswer.isBlank()) {
            model.addAttribute("error",
                    "Please select a recovery question and enter its answer.");
            return "signup";
        }

        if (recoveryQuestion.length() > 100) {
            model.addAttribute("error",
                    "Please select a valid recovery question.");
            return "signup";
        }

        try {
            AppUser user = new AppUser();
            user.setUsername(cleanUsername);
            user.setPassword(passwordEncoder.encode(password));
            user.setRole("VIEWER");

            // Save the recovery question and hashed answer.
            recoveryService.saveRecoveryQuestion(
                    user,
                    recoveryQuestion.trim(),
                    recoveryAnswer
            );

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Account created successfully. Please log in."
            );

            return "redirect:/login";

        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return "signup";
        }
    }
}
