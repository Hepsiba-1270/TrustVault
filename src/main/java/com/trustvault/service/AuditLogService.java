package com.trustvault.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.trustvault.entity.AuditLog;
import com.trustvault.repository.AuditLogRepository;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void logEvent(
            String username,
            String action,
            Long documentId,
            String documentName) {

        AuditLog log = new AuditLog();

        log.setUsername(username);
        log.setAction(action);
        log.setDocumentId(documentId);
        log.setDocumentName(documentName);
        log.setTimestamp(LocalDateTime.now());

        auditLogRepository.save(log);
        
        System.out.println(
                "AUDIT SAVED: " + action
                + " | User: " + username
                + " | Document ID: " + documentId);
    }
}