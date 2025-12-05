package uk.gov.hmcts.reform.em.npa.functional;

import net.serenitybdd.annotations.WithTag;
import net.serenitybdd.annotations.WithTags;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.em.EmTestConfig;
import uk.gov.hmcts.reform.em.npa.testutil.TestUtil;
import uk.gov.hmcts.reform.em.test.retry.RetryExtension;

import java.util.UUID;

@SpringBootTest(classes = {TestUtil.class, EmTestConfig.class})
@TestPropertySource(value = "classpath:application.yml")
@ExtendWith({SerenityJUnit5Extension.class, SpringExtension.class})
@WithTags({@WithTag("testType:Functional")})
class OpenIdConnectScenariosTest {

    private static final String MARKUPS_PATH = "/api/markups";

    private final TestUtil testUtil;

    @Value("${test.url}")
    private String testUrl;

    @RegisterExtension
    RetryExtension retryExtension = new RetryExtension(3);

    @Autowired
    public OpenIdConnectScenariosTest(TestUtil testUtil) {
        this.testUtil = testUtil;
    }

    @Test
    void testValidAuthenticationAndAuthorisation() {

        final String redactionId = UUID.randomUUID().toString();
        final String documentId = UUID.randomUUID().toString();
        final String rectangleId = UUID.randomUUID().toString();

        final JSONObject jsonObject = testUtil.createMarkUpPayload(redactionId, documentId, rectangleId);

        testUtil.authRequest()
            .baseUri(testUrl)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(jsonObject.toString())
            .post(MARKUPS_PATH)
            .then()
            .statusCode(201);
    }

    // Invalid S2SAuth
    @Test
    void testInvalidS2SAuth() {
        final String documentId = UUID.randomUUID().toString();
        testUtil.invalidS2SAuth()
            .baseUri(testUrl)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .when()
            .get(MARKUPS_PATH + "/" + documentId)
            .then()
            .statusCode(401);
    }

    //Invalid  IdamAuth
    @Test
    void testWithInvalidIdamAuth() {
        final String documentId = UUID.randomUUID().toString();
        testUtil.invalidIdamAuthrequest()
            .baseUri(testUrl)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .when()
            .get(MARKUPS_PATH + "/" + documentId)
            .then()
            .statusCode(401);
    }

    // S2S Auth valid, but missing Idam Auth
    @Test
    void testMissingIdamTokenShouldReturn401() {
        final String documentId = UUID.randomUUID().toString();
        testUtil.s2sAuthRequest()
            .baseUri(testUrl)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .when()
            .get(MARKUPS_PATH + "/" + documentId)
            .then()
            .statusCode(401);
    }

    // Idam Auth valid, but missing S2S Auth
    @Test
    void testMissingS2sTokenShouldReturn401() {
        final String documentId = UUID.randomUUID().toString();
        testUtil.idamOnlyAuthRequest()
            .baseUri(testUrl)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .when()
            .get(MARKUPS_PATH + "/" + documentId)
            .then()
            .statusCode(401);
    }

    // Both Idam and S2S Auth are missing
    @Test
    void testMissingBothAuthTokensShouldReturn401() {
        testUtil.unauthenticatedRequest()
            .baseUri(testUrl)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .when()
            .get(MARKUPS_PATH)
            .then()
            .statusCode(401);
    }
}