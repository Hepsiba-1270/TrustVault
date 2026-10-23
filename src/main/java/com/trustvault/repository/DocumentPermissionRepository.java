
package com.trustvault.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.trustvault.entity.DocumentPermission;

public interface DocumentPermissionRepository
        extends JpaRepository<DocumentPermission, Long> {

    List<DocumentPermission>
        findByOwnerUsernameOrderByCreatedAtDesc(String ownerUsername);

    List<DocumentPermission>
        findBySharedWithUsernameOrderByCreatedAtDesc(
            String sharedWithUsername);

    Optional<DocumentPermission>
        findByDocumentIdAndSharedWithUsername(
            Long documentId,
            String sharedWithUsername);

    boolean existsByDocumentIdAndSharedWithUsername(
        Long documentId,
        String sharedWithUsername);

    void deleteByDocumentIdAndSharedWithUsername(
        Long documentId,
        String sharedWithUsername);
}
