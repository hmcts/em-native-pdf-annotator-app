package uk.gov.hmcts.reform.em.npa.functional;

import io.restassured.response.Response;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import net.thucydides.core.annotations.WithTag;
import net.thucydides.core.annotations.WithTags;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.em.EmTestConfig;
import uk.gov.hmcts.reform.em.npa.testutil.TestUtil;
import uk.gov.hmcts.reform.em.test.retry.RetryRule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = {TestUtil.class, EmTestConfig.class})
@TestPropertySource(value = "classpath:application.yml")
@RunWith(SpringIntegrationSerenityRunner.class)
@WithTags({@WithTag("testType:Functional")})
public class OpenIdConnectScenarios {

    @Autowired
    private TestUtil testUtil;

    @Value("${test.url}")
    private String testUrl;

    @Rule
    public RetryRule retryRule = new RetryRule(3);

    @Test
    public void testValidAuthenticationAndAuthorisation() {
        testUtil.authRequest()
                .baseUri(testUrl)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .get("/api/document-tasks")
                .then()
                .statusCode(200);
    }

    // Invalid S2SAuth
    @Test
    public void testInvalidS2SAuth() {

        Response response =
                testUtil.invalidS2SAuth()
                        .baseUri(testUrl)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .get("/api/document-tasks");

        assertEquals(401, response.getStatusCode());
    }

    //Invalid  IdamAuth
    @Test
    public void testWithInvalidIdamAuth() {

        Response response =
                testUtil.invalidIdamAuthrequest()
                        .baseUri(testUrl)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .get("/api/document-tasks");

        assertEquals(401, response.getStatusCode());
    }

    //Empty S2SAuth
    @Test
    public void testWithEmptyS2SAuth() {

        assertThrows(NullPointerException.class, () -> testUtil
                .validAuthRequestWithEmptyS2SAuth()
                .baseUri(testUrl)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .get("/api/document-tasks"));
    }

    // Empty IdamAuth and Valid S2S Auth
    @Test
    public void testWithEmptyIdamAuthAndValidS2SAuth() {

        Throwable exceptionThrown =
                assertThrows(NullPointerException.class, () -> testUtil
                        .validS2SAuthWithEmptyIdamAuth()
                        .baseUri(testUrl)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .get("/api/document-tasks"));

        assertEquals("Header value", exceptionThrown.getMessage());
    }

    // Empty IdamAuth and Empty S2SAuth
    @Test
    public void testIdamAuthAndS2SAuthAreEmpty() {

        assertThrows(NullPointerException.class, () -> testUtil
                .emptyIdamAuthAndEmptyS2SAuth()
                .baseUri(testUrl)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .get("/api/document-tasks"));
    }
}
