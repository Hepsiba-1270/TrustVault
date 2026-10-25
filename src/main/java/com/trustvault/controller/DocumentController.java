
package com.trustvault.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
            @RequestParam(value = "visibility", defaultValue = "PRIVATE")
            String visibility,
            Authentication authentication) throws IOException {

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

        // Validate visibility before storing the file.
        String normalizedVisibility = visibility == null
                ? ""
                : visibility.trim().toUpperCase(Locale.ROOT);

        if (!"PUBLIC".equals(normalizedVisibility)
                && !"PRIVATE".equals(normalizedVisibility)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Visibility must be PUBLIC or PRIVATE.");
        }

        String contentType = file.getContentType();

        List<String> allowedTypes = List.of(
                "application/pdf",
                "text/plain",
                "image/png",
                "image/jpeg"
        );

        if (contentType == null
                || !allowedTypes.contains(contentType.toLowerCase(Locale.ROOT))) {
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

        // Store the physical file in private storage.
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
            document.setVisibility(normalizedVisibility);

            saved = documentRepository.save(document);

        } catch (RuntimeException ex) {
            Files.deleteIfExists(
                    storageService.getStoragePath(storageFilename));
            throw ex;
        }

        // Record the successful upload in the audit log.
        auditLogService.logEvent(
                authentication.getName(),
                "UPLOAD_" + normalizedVisibility,
                saved.getId(),
                saved.getOriginalFilename());

        Map<String, Object> response = new HashMap<>();

        response.put("message", "Upload successful");
        response.put("documentId", saved.getId());
        response.put("filename", saved.getOriginalFilename());
        response.put("owner", saved.getOwnerUsername());
        response.put("visibility", saved.getVisibility());

        return response;
    }

    // List documents owned by the logged-in user.
    @GetMapping
    public List<Document> myDocuments(Authentication authentication) {

        return documentRepository.findByOwnerUsername(
                authentication.getName());
    }

    // List public documents for any authenticated user.
    @GetMapping("/public")
    public List<Map<String, Object>> publicDocuments(
            Authentication authentication) {

        List<Map<String, Object>> result = new ArrayList<>();

        for (Document document : documentRepository.findAll()) {
            if (!"PUBLIC".equalsIgnoreCase(document.getVisibility())) {
                continue;
            }

            result.add(toDocumentSummary(document));
        }

        return result;
    }

    // List documents explicitly shared with the logged-in user.
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
                    documentRepository.findById(permission.getDocumentId());

            if (optionalDocument.isEmpty()) {
                continue;
            }

            Document doc = optionalDocument.get();

            // Ignore stale permission records if the owner does not match.
            if (!doc.getOwnerUsername().equals(permission.getOwnerUsername())) {
                continue;
            }

            Map<String, Object> item = toDocumentSummary(doc);
            item.put("permissionType", permission.getPermissionType());

            result.add(item);
        }

        return result;
    }

    // View a public document, an owned document, or a permitted shared document.
    @GetMapping("/{id}/view")
    public ResponseEntity<Resource> viewDocument(
            @PathVariable Long id,
            Authentication authentication) throws IOException {

        String username = authentication.getName();
        Document document = findDocument(id);

        boolean isOwner =
                document.getOwnerUsername().equals(username);

        boolean isPublic =
                "PUBLIC".equalsIgnoreCase(document.getVisibility());

        if (!isOwner && !isPublic) {
            Optional<DocumentPermission> permission =
                    permissionRepository
                            .findByDocumentIdAndSharedWithUsername(id, username);

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

    // Download a public document, an owned document,
    // or a shared document with DOWNLOAD permission.
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable Long id,
            Authentication authentication) throws IOException {

        String username = authentication.getName();
        Document document = findDocument(id);

        boolean isOwner =
                document.getOwnerUsername().equals(username);

        boolean isPublic =
                "PUBLIC".equalsIgnoreCase(document.getVisibility());

        if (!isOwner && !isPublic) {
            Optional<DocumentPermission> permission =
                    permissionRepository
                            .findByDocumentIdAndSharedWithUsername(id, username);

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

    // Create a safe summary without exposing the internal storage filename.
    private Map<String, Object> toDocumentSummary(Document document) {

        Map<String, Object> item = new HashMap<>();

        item.put("id", document.getId());
        item.put("name", document.getName());
        item.put("originalFilename", document.getOriginalFilename());
        item.put("ownerUsername", document.getOwnerUsername());
        item.put("size", document.getSize());
        item.put("uploadedAt", document.getUploadedAt());
        item.put("visibility", document.getVisibility());

        return item;
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

        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;

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

        // Preserve the original filename and its extension.
        String filename = document.getOriginalFilename();

        if (filename == null || filename.isBlank()) {
            filename = "document-" + document.getId();
        }

        // Remove unsafe characters from the response filename.
        filename = filename.replace("\"", "")
                .replace("/", "_")
                .replace("\\", "_");

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        disposition + "; filename=\"" + filename + "\"")
                .header("X-Content-Type-Options", "nosniff")
                .body(resource);
    }
}
