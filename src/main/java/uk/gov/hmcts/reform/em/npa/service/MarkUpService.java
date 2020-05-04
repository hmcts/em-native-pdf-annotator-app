package uk.gov.hmcts.reform.em.npa.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RedactionDTO;

import java.util.UUID;

public interface MarkUpService {

    /**
     * Save a markUp.
     *
     * @param redactionDTO the entity to save
     * @return the persisted entity
     */
    RedactionDTO save(RedactionDTO redactionDTO);

    /**
     *
     * @param pageable
     * @param documentId
     * @return
     */
    Page<RedactionDTO> findAllByDocumentId(UUID documentId, Pageable pageable);

    /**
     * Delete all the markUps of the document.
     *
     * @param documentId the id of the document
     */
    void deleteAll(UUID documentId);

    /**
     * Delete A markUp for the document.
     *
     * @param redactionId the id of the redaction
     */
    void delete(UUID redactionId);
}
