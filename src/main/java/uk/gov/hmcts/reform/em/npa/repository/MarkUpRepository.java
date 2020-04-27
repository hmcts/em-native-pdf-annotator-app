package uk.gov.hmcts.reform.em.npa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.em.npa.domain.MarkUp;

import java.util.UUID;

/**
 * Spring Data repository for the MarkUp entity.
 */
@Repository
public interface MarkUpRepository extends JpaRepository<MarkUp, UUID> {

    Page<MarkUp> findByDocumentIdAndCreatedBy(UUID documentId, String createdBy, Pageable pageable);

    @Modifying
    @Query(value = "DELETE FROM Markup m WHERE m.document_id = :documentId AND m.created_by = :createdBy", nativeQuery = true)
    void deleteAllByDocumentIdAndCreatedBy(@Param("documentId") UUID documentId, @Param("createdBy") String createdBy);

}
