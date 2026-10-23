
package com.trustvault.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.trustvault.entity.Document;

public interface DocumentRepository
        extends JpaRepository<Document, Long> {

    List<Document> findByOwnerUsername(String ownerUsername);

    List<Document> findByNameContainingIgnoreCase(String name);
}
