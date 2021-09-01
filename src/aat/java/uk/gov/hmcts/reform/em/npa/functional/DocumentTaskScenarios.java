package uk.gov.hmcts.reform.em.npa.functional;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import net.thucydides.core.annotations.WithTag;
import net.thucydides.core.annotations.WithTags;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.json.JSONObject;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.em.EmTestConfig;
import uk.gov.hmcts.reform.em.npa.domain.enumeration.TaskState;
import uk.gov.hmcts.reform.em.npa.testutil.TestUtil;
import uk.gov.hmcts.reform.em.test.retry.RetryRule;

import java.io.File;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@SpringBootTest(classes = {TestUtil.class, EmTestConfig.class})
@TestPropertySource(value = "classpath:application.yml")
@RunWith(SpringIntegrationSerenityRunner.class)
@WithTags({@WithTag("testType:Functional")})
@Ignore
public class DocumentTaskScenarios extends BaseTest {

    @Autowired
    private TestUtil testUtil;

    @Value("${test.url}")
    private String testUrl;

    @Rule
    public RetryRule retryRule = new RetryRule(3);

    private RequestSpecification request;

    @Before
    public void setupRequestSpecification() {
        request = testUtil
                .authRequest()
                .baseUri(testUrl)
                .contentType(APPLICATION_JSON_VALUE);
    }

    @Test
    public void testGetDocumentTasks() {
        testUtil
                .authRequest()
                .baseUri(testUrl)
                .get("/api/document-tasks")
                .then()
                .statusCode(200);
    }

    @Test
    public void testPostDocumentTaskDocumentNotFound() throws Exception {

        UUID nonExistentDocumentId = UUID.randomUUID();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("inputDocumentId", nonExistentDocumentId);

        request
                .body(jsonObject)
                .post("/api/document-tasks")
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

        request
                .body(jsonObject)
                .post("/api/document-tasks")
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

        request
                .body(jsonObject)
                .post("/api/document-tasks")
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

        Response response =
                request
                        .body(jsonObject)
                        .post("/api/document-tasks");

        Assert.assertEquals(201, response.getStatusCode());
        Assert.assertEquals(response.getBody().jsonPath().getString("inputDocumentId"), newDocId);
        Assert.assertEquals(response.getBody().jsonPath().getString("taskState"), TaskState.DONE.toString());

        File file = testUtil.getDocumentBinary(response.getBody().jsonPath().getString("outputDocumentId"));

        PDDocument pdDocument = PDDocument.load(file);

        PDPage page = pdDocument.getPage(0);

        Assert.assertNotNull(page.getAnnotations());
    }
}
