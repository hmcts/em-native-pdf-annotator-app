package uk.gov.hmcts.reform.em.npa.functional;

import io.restassured.RestAssured;
import org.json.JSONObject;
import org.junit.Test;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.em.npa.domain.enumeration.TaskState;
import uk.gov.hmcts.reform.em.npa.testutil.TestUtil;
import uk.gov.hmcts.reform.em.npa.testutil.Env;

import static org.hamcrest.CoreMatchers.*;

import java.util.UUID;

public class DocumentTaskScenarios {

    TestUtil testUtil = new TestUtil();

    @Test
    public void testGetDocumentTasks() throws Exception {

            testUtil.authRequest()
                .request("GET", Env.getTestUrl() + "/api/document-tasks")
            .then()
                .statusCode(200);

    }

    @Test
    public void testPostDocumentTaskDocumentNotFound() throws Exception {

        UUID nonExistentDocumentId = UUID.randomUUID();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("inputDocumentId", nonExistentDocumentId);

        testUtil.authRequest()
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(jsonObject)
                .request("POST", Env.getTestUrl() + "/api/document-tasks")
            .then()
                .statusCode(201)
                .body("inputDocumentId", equalTo(nonExistentDocumentId.toString()))
                .body("taskState", equalTo(TaskState.FAILED.toString()))
                .body("failureDescription", equalTo("Could not access the binary. HTTP response: 404"));

    }

    @Test
    public void testPostDocumentTaskAnnotationSetNotFound() throws Exception {

        String newDocId = testUtil.uploadDocument();

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("inputDocumentId", newDocId);

        testUtil.authRequest()
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(jsonObject)
                .request("POST", Env.getTestUrl() + "/api/document-tasks")
            .then()
                .statusCode(201)
                .body("inputDocumentId", equalTo(newDocId))
                .body("taskState", equalTo(TaskState.FAILED.toString()))
                .body("failureDescription", startsWith("Could not access the annotation set."));

    }

    @Test
    public void testPostDocumentTaskEmptyAnnotationSet() throws Exception {

        String newDocId = testUtil.uploadDocument();

        testUtil.createAnnotationSetForDocumentId(newDocId);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("inputDocumentId", newDocId);

        RestAssured
                .given()
                .header("ServiceAuthorization", "Bearer " + testUtil.getS2sToken())
                .header("Authorization", "Bearer " + testUtil.getIdamToken("test@test.com"))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(jsonObject)
                .request("POST", Env.getTestUrl() + "/api/document-tasks")
                .then()
                .statusCode(201)
                .body("inputDocumentId", equalTo(newDocId))
                .body("taskState", equalTo(TaskState.DONE.toString()));

    }

}
