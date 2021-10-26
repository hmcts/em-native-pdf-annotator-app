package uk.gov.hmcts.reform.em.npa.rest.errors;

public class EntityAuditEventException extends Exception {
    public EntityAuditEventException(String errorMessage) {
        super(errorMessage);
    }
}
