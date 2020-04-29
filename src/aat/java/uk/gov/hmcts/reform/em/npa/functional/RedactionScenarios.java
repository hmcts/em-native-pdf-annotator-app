package uk.gov.hmcts.reform.em.npa.functional;

import org.json.JSONObject;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.MarkUpDTO;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RedactionRequest;

import java.util.Arrays;
import java.util.UUID;

public class RedactionScenarios extends BaseTest {

    @Value("${test.url}")
    String testUrl;

    String newDocId;

    private MarkUpDTO createMarkUp() {
        MarkUpDTO markUpDTO = new MarkUpDTO();

        markUpDTO.setId(UUID.randomUUID());
        markUpDTO.setDocumentId(UUID.fromString(newDocId));
        markUpDTO.setPageNumber(1);
        markUpDTO.setHeight(100);
        markUpDTO.setWidth(100);
        markUpDTO.setXcoordinate(100);
        markUpDTO.setYcoordinate(100);

        return  markUpDTO;
    }

    @Test
    public void testSaveRedactedDocument() throws Exception {
        newDocId = testUtil.uploadDocument();
        String documentString = testUtil.getCcdDocumentJson("my doc", newDocId, "annotationTemplate.pdf");
        String caseId = testUtil.createCase(documentString).getId().toString();

        RedactionRequest redactionRequest = new RedactionRequest();
        redactionRequest.setCaseId(caseId);
        redactionRequest.setDocumentId(UUID.fromString(newDocId));
        redactionRequest.setMarkups(Arrays.asList(createMarkUp()));

        JSONObject jsonObject = new JSONObject(redactionRequest);

        testUtil.authRequest()
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(jsonObject)
                .request("POST", testUrl + "/api/redaction")
                .then()
                .statusCode(200);
    }
}
