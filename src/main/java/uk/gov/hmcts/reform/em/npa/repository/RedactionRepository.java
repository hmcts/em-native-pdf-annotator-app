package uk.gov.hmcts.reform.em.npa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.em.npa.domain.Redaction;

import java.util.List;
import java.util.UUID;

@Repository
public interface RedactionRepository extends JpaRepository<Redaction, Long> {
    List<Redaction> findByDocumentId(UUID documentId);
}
