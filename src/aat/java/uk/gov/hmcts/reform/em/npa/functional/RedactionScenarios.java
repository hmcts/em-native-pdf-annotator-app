package uk.gov.hmcts.reform.em.npa.functional;

import io.restassured.specification.RequestSpecification;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import net.thucydides.core.annotations.WithTag;
import net.thucydides.core.annotations.WithTags;
import org.json.JSONObject;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.em.EmTestConfig;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RedactionDTO;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RedactionRequest;
import uk.gov.hmcts.reform.em.npa.testutil.TestUtil;
import uk.gov.hmcts.reform.em.test.retry.RetryRule;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@SpringBootTest(classes = {TestUtil.class, EmTestConfig.class})
@TestPropertySource(value = "classpath:application.yml")
@RunWith(SpringIntegrationSerenityRunner.class)
@WithTags({@WithTag("testType:Functional")})
public class RedactionScenarios extends BaseTest {

    @Value("${test.url}")
    private String testUrl;

    @Autowired
    private TestUtil testUtil;

    @Rule
    public RetryRule retryRule = new RetryRule(3);

    private static final UUID documentId = UUID.randomUUID();
    private static final UUID redactionId = UUID.randomUUID();
    private RequestSpecification request;
    private RequestSpecification unAuthenticatedRequest;

    @Before
    public void setupRequestSpecification() {
        request = testUtil
                .authRequest()
                .baseUri(testUrl)
                .contentType(APPLICATION_JSON_VALUE);

        unAuthenticatedRequest = testUtil
                .unauthenticatedRequest()
                .baseUri(testUrl)
                .contentType(APPLICATION_JSON_VALUE);

        // If the Document Task Endpoint Toggle is enabled, continue, if not skip and ignore
        Assume.assumeTrue(toggleProperties.isEnableDocumentTaskEndpoint());
    }

    @Test
    public void shouldReturn200WhenRedactedPdfDocument() {
        final String newDocId = testUtil.uploadPdfDocumentAndReturnUrl();
        final RedactionRequest redactionRequest = new RedactionRequest();
        redactionRequest.setDocumentId(UUID.fromString(newDocId.substring(newDocId.lastIndexOf('/') + 1)));
        redactionRequest.setRedactions(Arrays.asList(createRedaction(), createRedaction()));

        final JSONObject jsonObject = new JSONObject(redactionRequest);

        request
                .body(jsonObject)
                .post("/api/redaction")
                .then()
                .assertThat()
                .statusCode(200)
                .body(notNullValue());
    }


    @Test
    public void shouldReturn200WhenRedactedImage() {
        final String newDocId = testUtil.uploadImageDocumentAndReturnUrl();
        final RedactionRequest redactionRequest = new RedactionRequest();
        redactionRequest.setDocumentId(UUID.fromString(newDocId.substring(newDocId.lastIndexOf('/') + 1)));
        redactionRequest.setRedactions(Collections.singletonList(createRedaction()));

        final JSONObject jsonObject = new JSONObject(redactionRequest);

        request
                .body(jsonObject)
                .post("/api/redaction")
                .then()
                .assertThat()
                .statusCode(200)
                .body(notNullValue());
    }

    @Test
    public void shouldReturn400WhenRedactedRichTextDocument() {
        final String newDocId = testUtil.uploadRichTextDocumentAndReturnUrl();
        final RedactionRequest redactionRequest = new RedactionRequest();
        redactionRequest.setDocumentId(UUID.fromString(newDocId.substring(newDocId.lastIndexOf('/') + 1)));
        redactionRequest.setRedactions(Arrays.asList(createRedaction(), createRedaction()));
        final JSONObject jsonObject = new JSONObject(redactionRequest);

        request
                .body(jsonObject)
                .post("/api/redaction")
                .then()
                .statusCode(400);
    }

    @Test
    public void shouldReturn400WhenRedactedFileNameIsMissing() {
        final String newDocId = testUtil.uploadRichTextDocumentAndReturnUrl();
        final RedactionRequest redactionRequest = new RedactionRequest();
        redactionRequest.setDocumentId(UUID.fromString(newDocId.substring(newDocId.lastIndexOf('/') + 1)));
        redactionRequest.setRedactions(Arrays.asList(createRedaction(), createRedaction()));
        final JSONObject jsonObject = new JSONObject(redactionRequest);

        request
                .body(jsonObject)
                .post("/api/redaction")
                .then()
                .statusCode(400);
    }

    @Test
    public void shouldReturn400WhenRedactedPdfDocumentWIthNonExistentDocumentId() {
        final UUID nonExistentDocId = UUID.randomUUID();
        final RedactionRequest redactionRequest = new RedactionRequest();
        redactionRequest.setDocumentId(nonExistentDocId);
        redactionRequest.setRedactions(Arrays.asList(createRedaction(), createRedaction()));

        final JSONObject jsonObject = new JSONObject(redactionRequest);

        request
                .body(jsonObject)
                .post("/api/redaction")
                .then()
                .assertThat()
                .statusCode(400)
                .body(notNullValue());
    }

    @Test
    public void shouldReturn401WhenRedactedPdfDocument() {
        final String newDocId = testUtil.uploadRichTextDocumentAndReturnUrl();
        final RedactionRequest redactionRequest = new RedactionRequest();
        redactionRequest.setDocumentId(UUID.fromString(newDocId.substring(newDocId.lastIndexOf('/') + 1)));
        redactionRequest.setRedactions(Arrays.asList(createRedaction(), createRedaction()));
        final JSONObject jsonObject = new JSONObject(redactionRequest);

        unAuthenticatedRequest
                .body(jsonObject)
                .post("/api/redaction")
                .then()
                .statusCode(401);
    }

    private RedactionDTO createRedaction() {
        final RedactionDTO redactionDTO = testUtil.createRedactionDTO(documentId, redactionId);
        redactionDTO.setPage(1);
        final JSONObject jsonObject = new JSONObject(redactionDTO);

        return request
                .body(jsonObject)
                .post("/api/markups")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .as(RedactionDTO.class);
    }

}
