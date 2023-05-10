package uk.gov.hmcts.reform.em.npa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.em.npa.domain.Redaction;

import java.util.UUID;

/**
 * Spring Data repository for the Redaction entity.
 */
@Repository
public interface MarkUpRepository extends JpaRepository<Redaction, Long> {

    Page<Redaction> findByDocumentIdAndCreatedBy(UUID documentId, String createdBy, Pageable pageable);

    @Transactional
    @Modifying
    @Query(
        value = "DELETE FROM Redaction m WHERE m.document_id = :documentId AND m.created_by = :createdBy",
        nativeQuery = true
    )
    void deleteAllByDocumentIdAndCreatedBy(@Param("documentId") UUID documentId, @Param("createdBy") String createdBy);

    void deleteByRedactionId(UUID redactionId);
}
