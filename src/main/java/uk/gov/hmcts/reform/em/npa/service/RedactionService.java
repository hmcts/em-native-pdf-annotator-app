package uk.gov.hmcts.reform.em.npa.service;

import uk.gov.hmcts.reform.em.npa.service.dto.redaction.MarkUpDTO;

import java.util.List;
import java.util.UUID;

public interface RedactionService {

    String redactFile(String jwt, String caseId, UUID documentId, List<MarkUpDTO> markUpDTOList);
}
