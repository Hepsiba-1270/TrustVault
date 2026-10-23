
package com.trustvault.service;

import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.trustvault.entity.Document;
import com.trustvault.entity.DocumentPermission;
import com.trustvault.repository.DocumentPermissionRepository;
import com.trustvault.repository.DocumentRepository;
import com.trustvault.repository.UserRepository;

@Service
public class DocumentPermissionService {

    private final DocumentPermissionRepository permissionRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public DocumentPermissionService(
            DocumentPermissionRepository permissionRepository,
            DocumentRepository documentRepository,
            UserRepository userRepository,
            AuditLogService auditLogService) {

        this.permissionRepository = permissionRepository;
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public DocumentPermission grantPermission(
            Long documentId,
            String sharedWithUsername,
            String permissionType,
            String ownerUsername) {

        if (sharedWithUsername == null
                || sharedWithUsername.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Please provide a username.");
        }

        sharedWithUsername = sharedWithUsername.trim();

        if (sharedWithUsername.equals(ownerUsername)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "You already own this document.");
        }

        String normalizedType = permissionType == null
                ? ""
                : permissionType.trim().toUpperCase(Locale.ROOT);

        if (!normalizedType.equals("VIEW")
                && !normalizedType.equals("DOWNLOAD")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Permission must be VIEW or DOWNLOAD.");
        }

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Document not found."));

        if (!document.getOwnerUsername().equals(ownerUsername)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the document owner can share this document.");
        }

        if (!userRepository.existsByUsername(sharedWithUsername)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The selected user does not exist.");
        }

        boolean alreadyShared = permissionRepository
                .existsByDocumentIdAndSharedWithUsername(
                        documentId, sharedWithUsername);

        DocumentPermission permission = permissionRepository
                .findByDocumentIdAndSharedWithUsername(
                        documentId, sharedWithUsername)
                .orElseGet(DocumentPermission::new);

        permission.setDocumentId(documentId);
        permission.setOwnerUsername(ownerUsername);
        permission.setSharedWithUsername(sharedWithUsername);
        permission.setPermissionType(normalizedType);

        DocumentPermission saved = permissionRepository.save(permission);

        String action = alreadyShared
                ? "UPDATE_PERMISSION_" + normalizedType
                : "SHARE_" + normalizedType;

        auditLogService.logEvent(
                ownerUsername,
                action,
                document.getId(),
                document.getOriginalFilename());

        return saved;
    }

    public List<DocumentPermission> getPermissionsForOwner(
            String ownerUsername) {

        return permissionRepository
                .findByOwnerUsernameOrderByCreatedAtDesc(ownerUsername);
    }

    public List<DocumentPermission> getPermissionsForUser(
            String username) {

        return permissionRepository
                .findBySharedWithUsernameOrderByCreatedAtDesc(username);
    }

    @Transactional
    public void revokePermission(
            Long permissionId,
            String ownerUsername) {

        DocumentPermission permission = permissionRepository
                .findById(permissionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Permission not found."));

        if (!permission.getOwnerUsername().equals(ownerUsername)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the owner can revoke this permission.");
        }

        Long documentId = permission.getDocumentId();

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Document not found."));

        String documentName = document.getOriginalFilename();
        permissionRepository.delete(permission);
        permissionRepository.flush();

        auditLogService.logEvent(
                ownerUsername,
                "REVOKE",
                documentId,
                documentName);
    }
    
    public boolean canViewDocument(Long documentId, String username) {

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Document not found."));

        // Owner can always view their own document.
        if (document.getOwnerUsername().equals(username)) {
            return true;
        }

        return permissionRepository
                .findByDocumentIdAndSharedWithUsername(documentId, username)
                .isPresent();
    }

    public boolean canDownloadDocument(Long documentId, String username) {

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Document not found."));

        // Owner can always download their own document.
        if (document.getOwnerUsername().equals(username)) {
            return true;
        }

        return permissionRepository
                .findByDocumentIdAndSharedWithUsername(documentId, username)
                .map(permission ->
                        "DOWNLOAD".equalsIgnoreCase(permission.getPermissionType()))
                .orElse(false);
    }
}
