
package com.trustvault.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.trustvault.entity.AuditLog;
import com.trustvault.repository.AuditLogRepository;

@Controller
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    public AuditController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    @ResponseBody
    public List<AuditLog> getAuditLogs(Authentication authentication) {

        boolean isAdmin = authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            return auditLogRepository.findAllByOrderByTimestampDesc();
        }

        return auditLogRepository.findByUsernameOrderByTimestampDesc(
                authentication.getName());
    }
}
