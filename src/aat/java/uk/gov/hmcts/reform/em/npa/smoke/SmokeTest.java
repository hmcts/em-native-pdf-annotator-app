package uk.gov.hmcts.reform.em.npa.smoke;

import io.restassured.RestAssured;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@TestPropertySource("classpath:application.yaml")
public class SmokeTest {

    private static final String MESSAGE = "Welcome to Native PDF Annotator API!";

    @Value("${test.url}")
    private String testUrl;

    @Test
    public void testHealthEndpoint() {
        RestAssured
                .given()
                .relaxedHTTPSValidation()
                .baseUri(testUrl)
                .get("/")
                .then()
                .log()
                .body();
    }
}
