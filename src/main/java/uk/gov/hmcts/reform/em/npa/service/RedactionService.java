package uk.gov.hmcts.reform.em.npa.service;

import com.sun.tools.javac.util.List;
import uk.gov.hmcts.reform.em.npa.service.dto.external.redaction.RedactionDTO;

import java.util.UUID;

public interface RedactionService {

    String redactFile(UUID documentId, List<RedactionDTO> redactionDTOList);
}
