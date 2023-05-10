package uk.gov.hmcts.reform.em.npa.smoke;

import net.serenitybdd.rest.SerenityRest;
import net.thucydides.core.annotations.WithTag;
import net.thucydides.core.annotations.WithTags;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

@TestPropertySource(value = "classpath:application.yml")
@WithTags({@WithTag("testType:Smoke")})
public class SmokeTest {

    private static final String MESSAGE = "Welcome to Native PDF Annotator API!";

    @Value("${test.url}")
    private String testUrl;

    @Test
    public void testHealthEndpoint() {

        SerenityRest.useRelaxedHTTPSValidation();

        Map responseMap =
                SerenityRest
                        .given()
                        .baseUri(testUrl)
                        .get("/")
                        .then()
                        .statusCode(200)
                        .extract()
                        .body()
                        .as(Map.class);

        Assert.assertEquals(1, responseMap.size());
        Assert.assertEquals(MESSAGE, responseMap.get("message"));

    }
}
