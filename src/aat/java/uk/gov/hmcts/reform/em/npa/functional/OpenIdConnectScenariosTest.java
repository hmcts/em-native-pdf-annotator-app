package uk.gov.hmcts.reform.em.npa.functional;

import io.restassured.response.Response;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = {TestUtil.class, EmTestConfig.class})
@TestPropertySource(value = "classpath:application.yml")
@ExtendWith({SerenityJUnit5Extension.class, SpringExtension.class})
@WithTags({@WithTag("testType:Functional")})
@SuppressWarnings("squid:S5778")
class OpenIdConnectScenariosTest {

    @Autowired
    private TestUtil testUtil;

    @Value("${test.url}")
    private String testUrl;

    @RegisterExtension
    RetryExtension retryExtension = new RetryExtension(3);

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
                .post("/api/markups")
                .then()
                .statusCode(201);
    }

    // Invalid S2SAuth
    @Test
    void testInvalidS2SAuth() {

        Response response =
                testUtil.invalidS2SAuth()
                        .baseUri(testUrl)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .get("/api/markups/");

        assertEquals(401, response.getStatusCode());
    }

    //Invalid  IdamAuth
    @Test
    void testWithInvalidIdamAuth() {

        Response response =
                testUtil.invalidIdamAuthrequest()
                        .baseUri(testUrl)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .get("/api/markups/");

        assertEquals(401, response.getStatusCode());
    }

    //Empty S2SAuth
    @Test
    void testWithEmptyS2SAuth() {

        assertThrows(NullPointerException.class, () -> testUtil
                .validAuthRequestWithEmptyS2SAuth()
                .baseUri(testUrl)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .get("/api/markups/"));
    }

    // Empty IdamAuth and Valid S2S Auth
    @Test
    void testWithEmptyIdamAuthAndValidS2SAuth() {

        Throwable exceptionThrown =
                assertThrows(NullPointerException.class, () -> testUtil
                        .validS2SAuthWithEmptyIdamAuth()
                        .baseUri(testUrl)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .get("/api/markups/"));

        assertEquals("Header value", exceptionThrown.getMessage());
    }

    // Empty IdamAuth and Empty S2SAuth
    @Test
    void testIdamAuthAndS2SAuthAreEmpty() {

        assertThrows(NullPointerException.class, () -> testUtil
                .emptyIdamAuthAndEmptyS2SAuth()
                .baseUri(testUrl)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .get("/api/markups/"));
    }
}
