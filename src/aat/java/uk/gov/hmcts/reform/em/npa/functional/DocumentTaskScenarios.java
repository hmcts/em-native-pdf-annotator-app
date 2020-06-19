package uk.gov.hmcts.reform.em.npa.functional;

import io.restassured.response.Response;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.em.EmTestConfig;
import uk.gov.hmcts.reform.em.npa.domain.enumeration.TaskState;
import uk.gov.hmcts.reform.em.npa.testutil.TestUtil;

import java.io.File;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;

@SpringBootTest(classes = {TestUtil.class, EmTestConfig.class})
@PropertySource(value = "classpath:application.yml")
@RunWith(SpringIntegrationSerenityRunner.class)

public class DocumentTaskScenarios {

    @Autowired
    TestUtil testUtil;

    @Value("${test.url}")
    String testUrl;

    @Test
    public void testGetDocumentTasks() throws Exception {

        testUtil.authRequest()
            .request("GET", testUrl + "/api/document-tasks")
            .then()
            .statusCode(200);

    }

    @Test
    public void testPostDocumentTaskDocumentNotFound() throws Exception {

        UUID nonExistentDocumentId = UUID.randomUUID();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("inputDocumentId", nonExistentDocumentId);

        testUtil
            .authRequest()
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .body(jsonObject)
            .request("POST", testUrl + "/api/document-tasks")
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

        testUtil
            .authRequest()
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .body(jsonObject)
            .request("POST", testUrl + "/api/document-tasks")
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

        testUtil.authRequest()
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .body(jsonObject)
            .request("POST", testUrl + "/api/document-tasks")
            .then()
            .statusCode(201)
            .body("inputDocumentId", equalTo(newDocId))
            .body("taskState", equalTo(TaskState.DONE.toString()));

    }

    @Test
    public void testPostDocumentTaskNotEmptyAnnotationSet() throws Exception {

        String newDocId = testUtil.uploadDocument();

        String annotationSetId = testUtil.createAnnotationSetForDocumentId(newDocId);

        testUtil.saveAnnotation(annotationSetId);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("inputDocumentId", newDocId);

        Response response = testUtil.authRequest()
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .body(jsonObject)
            .request("POST", testUrl + "/api/document-tasks");

        Assert.assertEquals(201, response.getStatusCode());
        Assert.assertEquals( response.getBody().jsonPath().getString("inputDocumentId"), newDocId);
        Assert.assertEquals( response.getBody().jsonPath().getString("taskState"), TaskState.DONE.toString());

        File file = testUtil.getDocumentBinary(response.getBody().jsonPath().getString("outputDocumentId"));

        PDDocument pdDocument = PDDocument.load(file);

        PDPage page = pdDocument.getPage(0);

        Assert.assertNotNull(page.getAnnotations());

    }

}
