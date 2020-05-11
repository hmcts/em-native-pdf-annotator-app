package uk.gov.hmcts.reform.em.npa.functional;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.em.EmTestConfig;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RedactionDTO;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RedactionRequest;
import uk.gov.hmcts.reform.em.npa.testutil.TestUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

@SpringBootTest(classes = {TestUtil.class, EmTestConfig.class})
@PropertySource(value = "classpath:application.yml")
@RunWith(SpringRunner.class)
public class RedactionScenarios {

    @Value("${test.url}")
    String testUrl;

    @Autowired
    protected TestUtil testUtil;

    private static final UUID docId = UUID.randomUUID();
    private static final UUID redactionId = UUID.randomUUID();

    private RedactionDTO createRedaction() {
        RedactionDTO redactionDTO = testUtil.createRedactionDTO(docId, redactionId);
        redactionDTO.setPage(1);

        JSONObject jsonObject = new JSONObject(redactionDTO);

        RedactionDTO response = testUtil.authRequest()
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(jsonObject)
                .request("POST", testUrl + "/api/markups")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .as(RedactionDTO.class);

        return response;
    }

    @Test
    public void testSaveRedactedPdfDocument() {
        String newDocId = testUtil.uploadPdfDocumentAndReturnUrl();

        RedactionRequest redactionRequest = new RedactionRequest();
        redactionRequest.setDocumentId(UUID.fromString(newDocId.substring(newDocId.lastIndexOf('/') + 1)));
        redactionRequest.setRedactions(Arrays.asList(createRedaction(), createRedaction()));

        JSONObject jsonObject = new JSONObject(redactionRequest);

        testUtil.authRequest()
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(jsonObject)
                .request("POST", testUrl + "/api/redaction")
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

        testUtil.authRequest()
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(jsonObject)
                .request("POST", testUrl + "/api/redaction")
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

        testUtil.authRequest()
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(jsonObject)
                .request("POST", testUrl + "/api/redaction")
                .then()
                .statusCode(400);
    }
}
