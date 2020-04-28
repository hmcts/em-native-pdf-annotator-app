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
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.em.EmTestConfig;
import uk.gov.hmcts.reform.em.npa.domain.MarkUpDTO;
import uk.gov.hmcts.reform.em.npa.domain.RedactionRequest;
import uk.gov.hmcts.reform.em.npa.testutil.ExtendedCcdHelper;
import uk.gov.hmcts.reform.em.npa.testutil.TestUtil;

import java.util.Arrays;
import java.util.UUID;

@SpringBootTest(classes = {TestUtil.class, EmTestConfig.class, ExtendedCcdHelper.class})
@PropertySource(value = "classpath:application.yml")
@RunWith(SpringRunner.class)
public class RedactionScenarios {

    @Autowired
    TestUtil testUtil;

    @Autowired
    ExtendedCcdHelper extendedCcdHelper;

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
        markUpDTO.setXCoordinate(100);
        markUpDTO.setYCoordinate(100);

        return  markUpDTO;
    }

    @Test
    public void testSaveRedactedDocument() throws Exception {
        newDocId = testUtil.uploadDocument();
        String documentString = extendedCcdHelper.getCcdDocumentJson("my doc", newDocId, "annotationTemplate.pdf");
        CaseDetails caseDetails = extendedCcdHelper.createCase(documentString);
        System.out.println(caseDetails);
        Long caseIdLong = caseDetails.getId();
        System.out.println(caseIdLong);
        String caseId = caseIdLong.toString();
        System.out.println(caseId);

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
