package uk.gov.hmcts.reform.em.npa.smoke;

import io.restassured.RestAssured;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;

public class SmokeTest {

    @Value("${test.url}")
    String testUrl;

    @Test
    public void testHealthEndpoint() {

        RestAssured.useRelaxedHTTPSValidation();

        RestAssured.given()
            .request("GET", testUrl + "/health")
            .then()
            .statusCode(200);


    }
}
