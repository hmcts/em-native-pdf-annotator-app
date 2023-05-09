package uk.gov.hmcts.reform.em.npa.smoke;

import net.serenitybdd.rest.SerenityRest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Map;

@ExtendWith(SpringExtension.class)
@TestPropertySource("classpath:application.yaml")
public class SmokeTest {

    private static final String MESSAGE = "Welcome to Native PDF Annotator API!";
    private final Logger log = LoggerFactory.getLogger(SmokeTest.class);

    @Value("${test.url}")
    private String testUrl;

    @Test
    public void testHealthEndpoint() {

        SerenityRest.useRelaxedHTTPSValidation();

        log.info("SmokeTest testUrl ===> {}", testUrl);
        try {
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
        } catch (Exception ex) {
            log.error("SmokeTest error:{}", ex.getMessage());
        }
    }
}
