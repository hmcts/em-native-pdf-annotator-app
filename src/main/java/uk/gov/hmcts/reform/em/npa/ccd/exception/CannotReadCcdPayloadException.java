package uk.gov.hmcts.reform.em.npa.ccd.exception;

public class CannotReadCcdPayloadException extends RuntimeException {

    public CannotReadCcdPayloadException(String message) {
        super(message);
    }

    public CannotReadCcdPayloadException(String message, Throwable cause) {
        super(message, cause);
    }
}
