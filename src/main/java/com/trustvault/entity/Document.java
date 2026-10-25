
package com.trustvault.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Locale;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String originalFilename;

    @Column(nullable = false)
    private String storageFilename;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private Long size;

    @Column(nullable = false)
    private String ownerUsername;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    @Column(nullable = false, length = 10)
    private String visibility = "PRIVATE";

    public Document() {
    }

    @PrePersist
    public void beforeSave() {
        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }

        if (visibility == null || visibility.isBlank()) {
            visibility = "PRIVATE";
        }

        visibility = visibility.trim().toUpperCase(Locale.ROOT);
        validateVisibility(visibility);
    }

    @PreUpdate
    public void beforeUpdate() {
        if (visibility == null || visibility.isBlank()) {
            visibility = "PRIVATE";
        }

        visibility = visibility.trim().toUpperCase(Locale.ROOT);
        validateVisibility(visibility);
    }

    private void validateVisibility(String value) {
        if (!"PUBLIC".equals(value) && !"PRIVATE".equals(value)) {
            throw new IllegalArgumentException(
                    "Visibility must be PUBLIC or PRIVATE.");
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getStorageFilename() {
        return storageFilename;
    }

    public void setStorageFilename(String storageFilename) {
        this.storageFilename = storageFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        if (visibility == null || visibility.isBlank()) {
            this.visibility = "PRIVATE";
            return;
        }

        String normalized = visibility.trim().toUpperCase(Locale.ROOT);
        validateVisibility(normalized);
        this.visibility = normalized;
    }
}
