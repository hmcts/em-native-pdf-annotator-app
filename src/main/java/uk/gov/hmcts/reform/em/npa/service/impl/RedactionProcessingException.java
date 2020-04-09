package uk.gov.hmcts.reform.em.npa.service.impl;

public class RedactionProcessingException extends RuntimeException {
    public RedactionProcessingException(Exception e) {
        super(e);
    }

    public RedactionProcessingException(String message) {
        super(message);
    }
}
