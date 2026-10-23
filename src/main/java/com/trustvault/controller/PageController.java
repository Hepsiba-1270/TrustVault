
package com.trustvault.controller;

import org.springframework.stereotype.Controller;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Controller
public class PageController {

    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/documents")
    public String documents() {
        return "documents";
    }

    @GetMapping("/permissions")
    public String permissions() {
        return "permissions";
    }

    @GetMapping("/audit")
    public String audit() {
        return "audit";
    }
    
    @GetMapping("/debug-auth")
    @ResponseBody
    public String debugAuth(Authentication authentication) {
        return "Username: " + authentication.getName()
                + " | Authorities: " + authentication.getAuthorities();
    }

    @GetMapping("/admin")
    public String admin() {
        return "admin";
    }
}
