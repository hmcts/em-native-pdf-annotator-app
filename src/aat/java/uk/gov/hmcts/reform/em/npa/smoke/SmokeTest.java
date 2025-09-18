package uk.gov.hmcts.reform.em.npa.smoke;

import net.serenitybdd.annotations.WithTag;
import net.serenitybdd.annotations.WithTags;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import net.serenitybdd.rest.SerenityRest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsMapWithSize.aMapWithSize;

@TestPropertySource(value = "classpath:application.yml")
@ExtendWith({SerenityJUnit5Extension.class, SpringExtension.class})
@WithTags({@WithTag("testType:Smoke")})
class SmokeTest {

    private static final String MESSAGE = "Welcome to Native PDF Annotator API!";

    @Value("${test.url}")
    private String testUrl;

    @Test
    void testHealthEndpoint() {

        SerenityRest.useRelaxedHTTPSValidation();

        SerenityRest
            .given()
            .baseUri(testUrl)
            .when()
            .get("/")
            .then()
            .statusCode(200)
            .body("$", aMapWithSize(1))
            .body("message", equalTo(MESSAGE));
    }
}