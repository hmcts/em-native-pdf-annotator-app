package uk.gov.hmcts.reform.em.npa.service.dto.redaction;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.UUID;

@Setter
@Getter
@ToString
public class RedactionRequest {
    private String caseId;
    private UUID documentId;
    private String redactedFileName;
    private List<RedactionDTO> redactions;
}
