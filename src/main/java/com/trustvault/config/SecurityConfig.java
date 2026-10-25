
package com.trustvault.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
            .authorizeHttpRequests(auth -> auth

                // Public pages and static resources.
                .requestMatchers(
                    "/login",
                    "/signup",
                    "/forgot-password",
                    "/reset-password",
                    "/error",
                    "/css/**",
                    "/js/**",
                    "/images/**"
                ).permitAll()

                // Public password recovery endpoints.
                // Keep only endpoints that actually exist in your controller.
                .requestMatchers(
                    "/api/account/reset-by-question",
                    "/api/account/reset-by-code"
                ).permitAll()

                // Only ADMIN users can access admin pages.
                .requestMatchers("/admin", "/admin/**")
                    .hasRole("ADMIN")

                // Only ADMIN users can access admin APIs.
                .requestMatchers("/api/admin/**")
                    .hasRole("ADMIN")

                // All other requests require authentication.
                .anyRequest().authenticated()
            )

            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )

            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .permitAll()
            );

        // CSRF protection remains enabled.
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
