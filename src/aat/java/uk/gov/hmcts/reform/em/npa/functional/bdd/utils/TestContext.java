package uk.gov.hmcts.reform.em.npa.functional.bdd.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TestContext {

    @Autowired
    private HttpContext httpContext;

    public HttpContext getHttpContext() {
        return httpContext;
    }
}