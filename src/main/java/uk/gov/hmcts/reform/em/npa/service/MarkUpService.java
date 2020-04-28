package uk.gov.hmcts.reform.em.npa.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.MarkUpDTO;

import java.util.UUID;

public interface MarkUpService {

    /**
     * Save a markUp.
     *
     * @param MarkUpDTO the entity to save
     * @return the persisted entity
     */
    MarkUpDTO save(MarkUpDTO MarkUpDTO);

    /**
     *
     * @param pageable
     * @param documentId
     * @return
     */
    Page<MarkUpDTO> findAllByDocumentId(UUID documentId, Pageable pageable);

    /**
     * Delete the "id" markUp.
     *
     * @param id the id of the entity
     */
    void delete(UUID id);
}
