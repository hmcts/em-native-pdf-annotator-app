package uk.gov.hmcts.reform.em.npa.ccd.domain;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class CcdCaseDocument {
    private String documentName;
    private String documentType;
    private CcdDocument documentLink;
    private LocalDateTime createdDatetime;
    private long size;
    private String createdBy;
}
