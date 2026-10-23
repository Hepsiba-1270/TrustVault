
package com.trustvault.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminAccessController {

    @GetMapping("/access-info")
    public Map<String, Object> getAccessInfo(Authentication authentication) {

        Map<String, Object> info = new LinkedHashMap<>();

        info.put("username", authentication.getName());

        info.put(
            "roles",
            authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList()
        );

        info.put("adminAccess", authentication.getAuthorities()
            .stream()
            .anyMatch(authority ->
                "ROLE_ADMIN".equals(authority.getAuthority())
            )
        );

        info.put("securityStatus", "Authenticated session");

        return info;
    }
}
