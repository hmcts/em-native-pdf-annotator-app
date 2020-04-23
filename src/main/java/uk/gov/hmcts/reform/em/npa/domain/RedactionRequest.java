package uk.gov.hmcts.reform.em.npa.domain;

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
    private List<MarkUpDTO> markups;
}
