package uk.gov.hmcts.reform.em.npa.service.impl;

public class DocumentTaskProcessingException extends RuntimeException {

    public DocumentTaskProcessingException(String message) {
        super(message);
    }

    public DocumentTaskProcessingException(String message, Throwable throwable) {
        super(message, throwable);
    }

}
