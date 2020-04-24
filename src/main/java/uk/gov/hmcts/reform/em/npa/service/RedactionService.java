package uk.gov.hmcts.reform.em.npa.service;

import uk.gov.hmcts.reform.em.npa.domain.RedactionDTO;

import java.util.List;
import java.util.UUID;

public interface RedactionService {

    String redactFile(UUID documentId, List<RedactionDTO> redactionDTOList);
}
