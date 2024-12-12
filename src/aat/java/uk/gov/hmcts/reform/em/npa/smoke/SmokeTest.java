package uk.gov.hmcts.reform.em.npa.smoke;

import net.serenitybdd.annotations.WithTag;
import net.serenitybdd.annotations.WithTags;
import net.serenitybdd.rest.SerenityRest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestPropertySource(value = "classpath:application.yml")
@SpringBootTest
@WithTags({@WithTag("testType:Smoke")})
class SmokeTest {

    private static final String MESSAGE = "Welcome to Native PDF Annotator API!";

    @Value("${test.url}")
    private String testUrl;

    @Test
    void testHealthEndpoint() {

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

        assertEquals(1, responseMap.size());
        assertEquals(MESSAGE, responseMap.get("message"));

    }
}
