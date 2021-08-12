package uk.gov.hmcts.reform.em.npa.service;

import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RedactionRequest;

import java.io.File;

public interface RedactionService {
    File redactFile(String auth, String serviceAuth, RedactionRequest redactionRequest);
}
