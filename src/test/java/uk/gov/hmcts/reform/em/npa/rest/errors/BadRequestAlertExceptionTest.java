package uk.gov.hmcts.reform.em.npa.rest.errors;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

class BadRequestAlertExceptionTest {

    @Test
    void testException() {
        BadRequestAlertException exception = new BadRequestAlertException("message", "entityName", "errorKey");

        Assert.assertEquals("entityName", exception.getEntityName());
        Assert.assertEquals("errorKey", exception.getErrorKey());
        Assert.assertEquals("message", exception.getMessage());

    }
}