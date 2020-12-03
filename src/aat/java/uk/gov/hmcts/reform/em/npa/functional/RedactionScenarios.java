package uk.gov.hmcts.reform.em.npa.functional;

import io.restassured.specification.RequestSpecification;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import net.thucydides.core.annotations.WithTag;
import net.thucydides.core.annotations.WithTags;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.em.EmTestConfig;
import uk.gov.hmcts.reform.em.npa.retry.RetryRule;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RedactionDTO;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RedactionRequest;
import uk.gov.hmcts.reform.em.npa.testutil.TestUtil;
import uk.gov.hmcts.reform.em.test.retry.RetryRule;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@SpringBootTest(classes = {TestUtil.class, EmTestConfig.class})
@TestPropertySource(value = "classpath:application.yml")
@RunWith(SpringIntegrationSerenityRunner.class)
@WithTags({@WithTag("testType:Functional")})
public class RedactionScenarios {

    @Value("${test.url}")
    private String testUrl;

    @Autowired
    private TestUtil testUtil;

    @Rule
    public RetryRule retryRule = new RetryRule(3);

    private static final UUID docId = UUID.randomUUID();
    private static final UUID redactionId = UUID.randomUUID();

    private RequestSpecification request;

    @Before
    public void setupRequestSpecification() {
        request = testUtil
                .authRequest()
                .baseUri(testUrl)
                .contentType(APPLICATION_JSON_VALUE);
    }

    private RedactionDTO createRedaction() {
        RedactionDTO redactionDTO = testUtil.createRedactionDTO(docId, redactionId);
        redactionDTO.setPage(1);

        JSONObject jsonObject = new JSONObject(redactionDTO);

        return request
                .body(jsonObject)
                .post("/api/markups")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .as(RedactionDTO.class);
    }

    @Test
    public void testSaveRedactedPdfDocument() {
        String newDocId = testUtil.uploadPdfDocumentAndReturnUrl();

        RedactionRequest redactionRequest = new RedactionRequest();
        redactionRequest.setDocumentId(UUID.fromString(newDocId.substring(newDocId.lastIndexOf('/') + 1)));
        redactionRequest.setRedactions(Arrays.asList(createRedaction(), createRedaction()));

        JSONObject jsonObject = new JSONObject(redactionRequest);

        request
                .body(jsonObject)
                .post("/api/redaction")
                .then()
                .statusCode(200);
    }

    @Test
    public void testSaveRedactedImageDocument() {
        String newDocId = testUtil.uploadImageDocumentAndReturnUrl();

        RedactionRequest redactionRequest = new RedactionRequest();
        redactionRequest.setDocumentId(UUID.fromString(newDocId.substring(newDocId.lastIndexOf('/') + 1)));
        redactionRequest.setRedactions(Collections.singletonList(createRedaction()));

        JSONObject jsonObject = new JSONObject(redactionRequest);

        request
                .body(jsonObject)
                .post("/api/redaction")
                .then()
                .statusCode(200);
    }

    @Test
    public void testFailedSaveRedactedRichTextDocument() {
        String newDocId = testUtil.uploadRichTextDocumentAndReturnUrl();

        RedactionRequest redactionRequest = new RedactionRequest();
        redactionRequest.setDocumentId(UUID.fromString(newDocId.substring(newDocId.lastIndexOf('/') + 1)));
        redactionRequest.setRedactions(Arrays.asList(createRedaction(), createRedaction()));

        JSONObject jsonObject = new JSONObject(redactionRequest);

        request
                .body(jsonObject)
                .post("/api/redaction")
                .then()
                .statusCode(400);
    }
}
