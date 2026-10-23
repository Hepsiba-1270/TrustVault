
package com.trustvault.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.trustvault.entity.Document;
import com.trustvault.entity.DocumentPermission;
import com.trustvault.repository.DocumentPermissionRepository;
import com.trustvault.repository.DocumentRepository;
import com.trustvault.service.AuditLogService;
import com.trustvault.service.DocumentStorageService;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentRepository documentRepository;
    private final DocumentStorageService storageService;
    private final AuditLogService auditLogService;
    private final DocumentPermissionRepository permissionRepository;

    public DocumentController(
            DocumentRepository documentRepository,
            DocumentStorageService storageService,
            AuditLogService auditLogService,
            DocumentPermissionRepository permissionRepository) {

        this.documentRepository = documentRepository;
        this.storageService = storageService;
        this.auditLogService = auditLogService;
        this.permissionRepository = permissionRepository;
    }

    // Upload is allowed only for OWNER and ADMIN roles.
    @PostMapping("/upload")
    public Map<String, Object> upload(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws IOException {

        // Enforce the role restriction on the server.
        boolean canUpload = authentication != null
                && authentication.getAuthorities().stream()
                        .anyMatch(authority -> {
                            String role = authority.getAuthority();

                            if (role == null) {
                                return false;
                            }

                            if (role.startsWith("ROLE_")) {
                                role = role.substring(5);
                            }

                            return "OWNER".equalsIgnoreCase(role)
                                    || "ADMIN".equalsIgnoreCase(role);
                        });

        if (!canUpload) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only Owners and Admins can upload documents.");
        }

        // Validate the uploaded file.
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Please select a file.");
        }

        if (file.getSize() > 10 * 1024 * 1024) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Maximum file size is 10 MB.");
        }

        String contentType = file.getContentType();

        List<String> allowedTypes = List.of(
                "application/pdf",
                "text/plain",
                "image/png",
                "image/jpeg"
        );

        if (contentType == null
                || !allowedTypes.contains(contentType.toLowerCase())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only PDF, TXT, PNG and JPEG files are allowed.");
        }

        // Reject filenames that could contain path traversal.
        String originalName = file.getOriginalFilename();

        if (originalName == null
                || originalName.isBlank()
                || originalName.contains("/")
                || originalName.contains("\\")
                || originalName.contains("..")) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid filename.");
        }

        // Store the file privately.
        String storageFilename = storageService.storeFile(file);
        Document saved;

        try {
            Document document = new Document();

            document.setName(originalName);
            document.setOriginalFilename(originalName);
            document.setStorageFilename(storageFilename);
            document.setContentType(contentType);
            document.setSize(file.getSize());
            document.setOwnerUsername(authentication.getName());

            saved = documentRepository.save(document);

        } catch (RuntimeException ex) {
            // Delete the stored file if saving the database record fails.
            Files.deleteIfExists(
                    storageService.getStoragePath(storageFilename));
            throw ex;
        }

        // Record the successful upload in the audit log.
        auditLogService.logEvent(
                authentication.getName(),
                "UPLOAD",
                saved.getId(),
                saved.getOriginalFilename());

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Upload successful");
        response.put("documentId", saved.getId());
        response.put("filename", saved.getOriginalFilename());
        response.put("owner", saved.getOwnerUsername());

        return response;
    }

    // List documents owned by the logged-in user.
    @GetMapping
    public List<Document> myDocuments(
            Authentication authentication) {

        return documentRepository.findByOwnerUsername(
                authentication.getName());
    }

    // List documents shared with the logged-in user.
    // Do not expose the internal storage filename.
    @GetMapping("/shared-with-me")
    public List<Map<String, Object>> sharedDocuments(
            Authentication authentication) {

        String username = authentication.getName();

        List<DocumentPermission> permissions =
                permissionRepository
                        .findBySharedWithUsernameOrderByCreatedAtDesc(username);

        List<Map<String, Object>> result = new ArrayList<>();

        for (DocumentPermission permission : permissions) {

            Optional<Document> optionalDocument =
                    documentRepository.findById(
                            permission.getDocumentId());

            if (optionalDocument.isEmpty()) {
                continue;
            }

            Document doc = optionalDocument.get();

            // Ignore stale permission records if the owner does not match.
            if (!doc.getOwnerUsername().equals(
                    permission.getOwnerUsername())) {
                continue;
            }

            Map<String, Object> item = new HashMap<>();

            item.put("id", doc.getId());
            item.put("name", doc.getName());
            item.put("originalFilename", doc.getOriginalFilename());
            item.put("ownerUsername", doc.getOwnerUsername());
            item.put("size", doc.getSize());
            item.put("uploadedAt", doc.getUploadedAt());
            item.put("permissionType", permission.getPermissionType());

            result.add(item);
        }

        return result;
    }

    // The owner or a user with VIEW or DOWNLOAD permission can view.
    @GetMapping("/{id}/view")
    public ResponseEntity<Resource> viewDocument(
            @PathVariable Long id,
            Authentication authentication) throws IOException {

        String username = authentication.getName();
        Document document = findDocument(id);

        boolean isOwner =
                document.getOwnerUsername().equals(username);

        if (!isOwner) {

            Optional<DocumentPermission> permission =
                    permissionRepository
                            .findByDocumentIdAndSharedWithUsername(
                                    id, username);

            if (permission.isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "View permission is required.");
            }

            String type = permission.get().getPermissionType();

            if (!"VIEW".equalsIgnoreCase(type)
                    && !"DOWNLOAD".equalsIgnoreCase(type)) {

                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "View permission is required.");
            }
        }

        Path path = getExistingFilePath(document);

        auditLogService.logEvent(
                username,
                "VIEW",
                document.getId(),
                document.getOriginalFilename());

        return buildFileResponse(document, path, true);
    }

    // Only the owner or a user with DOWNLOAD permission can download.
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable Long id,
            Authentication authentication) throws IOException {

        String username = authentication.getName();
        Document document = findDocument(id);

        boolean isOwner =
                document.getOwnerUsername().equals(username);

        if (!isOwner) {

            Optional<DocumentPermission> permission =
                    permissionRepository
                            .findByDocumentIdAndSharedWithUsername(
                                    id, username);

            if (permission.isEmpty()
                    || !"DOWNLOAD".equalsIgnoreCase(
                            permission.get().getPermissionType())) {

                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Download permission is required.");
            }
        }

        Path path = getExistingFilePath(document);

        auditLogService.logEvent(
                username,
                "DOWNLOAD",
                document.getId(),
                document.getOriginalFilename());

        return buildFileResponse(document, path, false);
    }

    // Find a document or return 404.
    private Document findDocument(Long id) {

        return documentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Document not found."));
    }

    // Return the path only if the stored file exists.
    private Path getExistingFilePath(Document document) {

        Path path = storageService.getStoragePath(
                document.getStorageFilename());

        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Stored file not found.");
        }

        return path;
    }

    // Build a response for viewing or downloading a document.
    private ResponseEntity<Resource> buildFileResponse(
            Document document,
            Path path,
            boolean inline) throws IOException {

        MediaType mediaType =
                MediaType.APPLICATION_OCTET_STREAM;

        String contentType = document.getContentType();

        if (contentType != null) {
            try {
                mediaType = MediaType.parseMediaType(contentType);
            } catch (Exception ignored) {
                // Keep application/octet-stream as the safe default.
            }
        }

        Resource resource = new InputStreamResource(
                Files.newInputStream(path));

        String disposition = inline ? "inline" : "attachment";

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        disposition + "; filename=\"document-"
                                + document.getId() + "\"")
                .header("X-Content-Type-Options", "nosniff")
                .body(resource);
    }
}
