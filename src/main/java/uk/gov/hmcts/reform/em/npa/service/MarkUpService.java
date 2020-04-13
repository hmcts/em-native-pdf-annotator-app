package uk.gov.hmcts.reform.em.npa.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.reform.em.npa.service.dto.external.redaction.RedactionDTO;

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
     * Delete the "id" markUp.
     *
     * @param id the id of the entity
     */
    void delete(UUID id);
}
