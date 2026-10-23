
package com.trustvault.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.trustvault.entity.DocumentPermission;
import com.trustvault.service.DocumentPermissionService;

@RestController
@RequestMapping("/api/permissions")
public class PermissionController {

    private final DocumentPermissionService permissionService;

    public PermissionController(
            DocumentPermissionService permissionService) {
        this.permissionService = permissionService;
    }

    // Only OWNER can grant or update document permissions.
    @PostMapping
    public DocumentPermission grantPermission(
            @RequestBody PermissionRequest request,
            Authentication authentication) {

        requireOwner(authentication);

        if (request == null || request.getDocumentId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Document ID is required.");
        }

        if (request.getSharedWithUsername() == null
                || request.getSharedWithUsername().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Username is required.");
        }

        if (request.getPermissionType() == null
                || request.getPermissionType().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Permission type is required.");
        }

        return permissionService.grantPermission(
                request.getDocumentId(),
                request.getSharedWithUsername(),
                request.getPermissionType(),
                authentication.getName());
    }

    // Only OWNER can view permissions they granted.
    @GetMapping("/owner")
    public List<DocumentPermission> getOwnerPermissions(
            Authentication authentication) {

        requireOwner(authentication);

        return permissionService.getPermissionsForOwner(
                authentication.getName());
    }

    // Every authenticated role can view permissions granted to them.
    @GetMapping("/shared-with-me")
    public List<DocumentPermission> getSharedPermissions(
            Authentication authentication) {

        return permissionService.getPermissionsForUser(
                authentication.getName());
    }

    // Only OWNER can revoke permissions they own.
    @DeleteMapping("/{permissionId}")
    public Map<String, String> revokePermission(
            @PathVariable Long permissionId,
            Authentication authentication) {

        requireOwner(authentication);

        permissionService.revokePermission(
                permissionId,
                authentication.getName());

        return Map.of(
                "message",
                "Permission revoked successfully.");
    }

    // Verify that the logged-in user has ROLE_OWNER authority.
    private void requireOwner(Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Please log in first.");
        }

        boolean isOwner = authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_OWNER"));

        if (!isOwner) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only document owners can manage permissions.");
        }
    }

    // JSON request body used by the grant/update API.
    public static class PermissionRequest {

        private Long documentId;
        private String sharedWithUsername;
        private String permissionType;

        public PermissionRequest() {
        }

        public Long getDocumentId() {
            return documentId;
        }

        public void setDocumentId(Long documentId) {
            this.documentId = documentId;
        }

        public String getSharedWithUsername() {
            return sharedWithUsername;
        }

        public void setSharedWithUsername(
                String sharedWithUsername) {
            this.sharedWithUsername = sharedWithUsername;
        }

        public String getPermissionType() {
            return permissionType;
        }

        public void setPermissionType(String permissionType) {
            this.permissionType = permissionType;
        }
    }
}
