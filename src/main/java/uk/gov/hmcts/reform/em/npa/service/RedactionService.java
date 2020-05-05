package uk.gov.hmcts.reform.em.npa.service;

import uk.gov.hmcts.reform.em.npa.service.dto.redaction.MarkUpDTO;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RedactionDTO;

import java.util.List;
import java.util.UUID;

public interface RedactionService {

    String redactFile(String jwt, String caseId, UUID documentId, List<RedactionDTO> redactionDTOList);
}
