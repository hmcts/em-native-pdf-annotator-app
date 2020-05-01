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
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.MarkUpDTO;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RedactionRequest;
import uk.gov.hmcts.reform.em.npa.testutil.TestUtil;

import java.util.Arrays;
import java.util.UUID;

@SpringBootTest(classes = {TestUtil.class, EmTestConfig.class})
@PropertySource(value = "classpath:application.yml")
@RunWith(SpringRunner.class)
public class RedactionScenarios {

    @Value("${test.url}")
    String testUrl;

    @Autowired
    protected TestUtil testUtil;

    private static final UUID id = UUID.randomUUID();

    private MarkUpDTO createMarkUp() {
        MarkUpDTO markUpDTO = testUtil.populateMarkUpDTO(id);

        JSONObject jsonObject = new JSONObject(markUpDTO);

        MarkUpDTO response = testUtil.authRequest()
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(jsonObject)
                .request("POST", testUrl + "/api/markups")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .as(MarkUpDTO.class);

        return response;
    }

    @Test
    public void testSaveRedactedPdfDocument() throws Exception {
        String newDocId = testUtil.uploadPdfDocumentAndReturnUrl();
        String documentString = testUtil.getCcdDocumentJson("my doc", newDocId, "annotationTemplate.pdf");
        String caseId = testUtil.createCase(documentString).getId().toString();

        RedactionRequest redactionRequest = new RedactionRequest();
        redactionRequest.setCaseId(caseId);
        redactionRequest.setDocumentId(UUID.fromString(newDocId.substring(newDocId.lastIndexOf('/') + 1)));
        redactionRequest.setMarkups(Arrays.asList(createMarkUp()));

        JSONObject jsonObject = new JSONObject(redactionRequest);

        testUtil.authRequest()
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(jsonObject)
                .request("POST", testUrl + "/api/redaction")
                .then()
                .statusCode(200);
    }

//    @Test
//    public void testSaveRedactedImageDocument() throws Exception {
//        String newDocId = testUtil.uploadImageDocumentAndReturnUrl();
//        String documentString = testUtil.getCcdDocumentJson("my doc", newDocId, "annotationTemplate.pdf");
//        String caseId = testUtil.createCase(documentString).getId().toString();
//
//        RedactionRequest redactionRequest = new RedactionRequest();
//        redactionRequest.setCaseId(caseId);
//        redactionRequest.setDocumentId(UUID.fromString(newDocId.substring(newDocId.lastIndexOf('/') + 1)));
//        redactionRequest.setMarkups(Arrays.asList(createMarkUp()));
//
//        JSONObject jsonObject = new JSONObject(redactionRequest);
//
//        testUtil.authRequest()
//                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
//                .body(jsonObject)
//                .request("POST", testUrl + "/api/redaction")
//                .then()
//                .statusCode(200);
//    }

    @Test
    public void testFailedSaveRedactedRichTextDocument() throws Exception {
        String newDocId = testUtil.uploadRichTextDocumentAndReturnUrl();
        String documentString = testUtil.getCcdDocumentJson("my doc", newDocId, "annotationTemplate.pdf");
        String caseId = testUtil.createCase(documentString).getId().toString();

        RedactionRequest redactionRequest = new RedactionRequest();
        redactionRequest.setCaseId(caseId);
        redactionRequest.setDocumentId(UUID.fromString(newDocId.substring(newDocId.lastIndexOf('/') + 1)));
        redactionRequest.setMarkups(Arrays.asList(createMarkUp()));

        JSONObject jsonObject = new JSONObject(redactionRequest);

        testUtil.authRequest()
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(jsonObject)
                .request("POST", testUrl + "/api/redaction")
                .then()
                .statusCode(400);
    }

    @Test
    public void testSaveRedactedDocumentInvalidCCDCaseId() throws Exception {
        String newDocId = testUtil.uploadPdfDocumentAndReturnUrl();

        RedactionRequest redactionRequest = new RedactionRequest();
        redactionRequest.setCaseId("invalid_id");
        redactionRequest.setDocumentId(UUID.fromString(newDocId.substring(newDocId.lastIndexOf('/') + 1)));
        redactionRequest.setMarkups(Arrays.asList(createMarkUp()));

        JSONObject jsonObject = new JSONObject(redactionRequest);

        testUtil.authRequest()
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(jsonObject)
                .request("POST", testUrl + "/api/redaction")
                .then()
                .statusCode(400);
    }
}
