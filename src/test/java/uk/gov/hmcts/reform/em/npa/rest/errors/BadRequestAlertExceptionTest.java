package uk.gov.hmcts.reform.em.npa.rest.errors;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.mockito.Mockito.verify;

public class BadRequestAlertExceptionTest {

    @Autowired
    BadRequestAlertException badRequestAlertException;

    @Test
    public void testException() {

//        verify(badRequestAlertException.getEntityName());
//        Assert.assertEquals();

    }
}
