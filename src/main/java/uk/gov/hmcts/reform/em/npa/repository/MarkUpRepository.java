package uk.gov.hmcts.reform.em.npa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.em.npa.domain.MarkUp;

import java.util.UUID;

/**
 * Spring Data repository for the MarkUp entity.
 */
@SuppressWarnings("unused")
@Repository
public interface MarkUpRepository extends JpaRepository<MarkUp, UUID> {

    Page<MarkUp> findByDocumentIdAndCreatedBy(UUID documentId, String createdBy, Pageable pageable);

}
