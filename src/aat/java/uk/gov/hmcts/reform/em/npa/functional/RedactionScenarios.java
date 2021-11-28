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
import uk.gov.hmcts.reform.ccd.document.am.model.UploadResponse;
import uk.gov.hmcts.reform.em.EmTestConfig;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RedactionDTO;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RedactionRequest;
import uk.gov.hmcts.reform.em.npa.testutil.ExtendedCcdHelper;
import uk.gov.hmcts.reform.em.npa.testutil.TestUtil;
import uk.gov.hmcts.reform.em.test.retry.RetryRule;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@SpringBootTest(classes = {TestUtil.class, EmTestConfig.class, ExtendedCcdHelper.class})
@TestPropertySource(value = "classpath:application.yml")
@RunWith(SpringIntegrationSerenityRunner.class)
@WithTags({@WithTag("testType:Functional")})
public class RedactionScenarios {

    @Value("${test.url}")
    private String testUrl;

    @Autowired
    private TestUtil testUtil;

    @Autowired
    protected ExtendedCcdHelper extendedCcdHelper;

    @Value("${toggles.cdam_enabled}")
    boolean cdamEnabled;

    @Rule
    public RetryRule retryRule = new RetryRule(3);

    private static final UUID documentId = UUID.randomUUID();
    private static final UUID redactionId = UUID.randomUUID();
    private RequestSpecification request;
    private RequestSpecification cdamRequest;
    private RequestSpecification unAuthenticatedRequest;

    @Before
    public void setupRequestSpecification() {
        request = testUtil
                .authRequest()
                .baseUri(testUrl)
                .contentType(APPLICATION_JSON_VALUE);

        cdamRequest = testUtil
            .cdamAuthRequest()
            .baseUri(testUrl)
            .contentType(APPLICATION_JSON_VALUE);

        unAuthenticatedRequest = testUtil
                .unauthenticatedRequest()
                .baseUri(testUrl)
                .contentType(APPLICATION_JSON_VALUE);
    }

    @Test
    public void shouldReturn200WhenRedactedPdfDocument() {
        Assume.assumeFalse(cdamEnabled);
        System.out.println("cdamEnabled value is "+cdamEnabled);
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
    public void shouldReturn200WhenRedactedPdfDocumentCdamEnabled() throws Exception {
        Assume.assumeTrue(cdamEnabled);
        System.out.println("cdamEnabled value is "+cdamEnabled);
        UploadResponse uploadResponse = testUtil.uploadCdamDocument("a@b.com",
            extendedCcdHelper.getEnvCcdCaseTypeId(), "PUBLICLAW");

        String uploadedUrl = uploadResponse.getDocuments().get(0).links.self.href;
        String docHash = uploadResponse.getDocuments().get(0).hashToken;

        String documentString = extendedCcdHelper.getCcdDocumentJson("annotationTemplate", uploadedUrl,
            "annotationTemplate.pdf", docHash);

        extendedCcdHelper.createCase(documentString);

        String docId = uploadedUrl.substring(uploadResponse.getDocuments().get(0).links.self.href
            .lastIndexOf('/') + 1);
        final RedactionRequest redactionRequest = new RedactionRequest();
        redactionRequest.setDocumentId(UUID.fromString(docId));

        redactionRequest.setRedactions(Arrays.asList(createCdamRedaction(docId), createCdamRedaction(docId)));

        final JSONObject jsonObject = new JSONObject(redactionRequest);

        cdamRequest
            .body(jsonObject)
            .post("/api/redaction")
            .then()
            .assertThat()
            .statusCode(200)
            .body(notNullValue());

    }

    @Test
    public void shouldReturn400WhenRedactedPdfDocumentCdamEnabled() throws Exception {
        Assume.assumeTrue(cdamEnabled);
        System.out.println("cdamEnabled value is "+cdamEnabled);
        UploadResponse uploadResponse = testUtil.uploadCdamDocument("a@b.com",
            extendedCcdHelper.getEnvCcdCaseTypeId(), "PUBLICLAW");

        String uploadedUrl = uploadResponse.getDocuments().get(0).links.self.href;
        String docHash = uploadResponse.getDocuments().get(0).hashToken;

        String documentString = extendedCcdHelper.getCcdDocumentJson("annotationTemplate", uploadedUrl,
            "annotationTemplate.pdf", docHash);

        extendedCcdHelper.createCase(documentString);

        String docId = uploadedUrl.substring(uploadResponse.getDocuments().get(0).links.self.href
            .lastIndexOf('/') + 1);
        final RedactionRequest redactionRequest = new RedactionRequest();
        redactionRequest.setDocumentId(UUID.fromString(docId));

        redactionRequest.setRedactions(Arrays.asList(createCdamRedaction(docId), createCdamRedaction(docId)));

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
    public void shouldReturn200WhenRedactedImage() {
        Assume.assumeFalse(cdamEnabled);
        System.out.println("cdamEnabled value is "+cdamEnabled);
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
        Assume.assumeFalse(cdamEnabled);
        System.out.println("cdamEnabled value is "+cdamEnabled);
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
        Assume.assumeFalse(cdamEnabled);
        System.out.println("cdamEnabled value is "+cdamEnabled);
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
        Assume.assumeFalse(cdamEnabled);
        System.out.println("cdamEnabled value is "+cdamEnabled);
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
        Assume.assumeFalse(cdamEnabled);
        System.out.println("cdamEnabled value is "+cdamEnabled);
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

    private RedactionDTO createCdamRedaction(String documentId) {
        final RedactionDTO redactionDTO = testUtil.createRedactionDTO(UUID.fromString(documentId), redactionId);
        redactionDTO.setPage(1);
        final JSONObject jsonObject = new JSONObject(redactionDTO);

        return cdamRequest
            .body(jsonObject)
            .post("/api/markups")
            .then()
            .statusCode(201)
            .extract()
            .body()
            .as(RedactionDTO.class);
    }

}
