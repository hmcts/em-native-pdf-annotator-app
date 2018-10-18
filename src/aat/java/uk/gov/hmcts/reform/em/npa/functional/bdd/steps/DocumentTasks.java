package uk.gov.hmcts.reform.em.npa.functional.bdd.steps;

import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import uk.gov.hmcts.reform.em.npa.domain.DocumentTask;
import uk.gov.hmcts.reform.em.npa.functional.bdd.utils.TestContext;
import uk.gov.hmcts.reform.em.npa.service.dto.DocumentTaskDTO;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

@ContextConfiguration
@SpringBootTest
@ActiveProfiles("cucumber")
public class DocumentTasks extends BaseSteps {

    @Autowired
    public DocumentTasks(TestContext testContext) {
        super(testContext);
    }

    @Before
    public void setUp() throws Exception {
        super.setup();
    }

    String documentTasksUrl = "/api/document-tasks";

    /**
     * Creates a question to be used for testing with an answer
     */
    @When("^GET document-tasks$")
    public void get_all_documents_tasks() throws Exception {
        HttpEntity entity = new HttpEntity(header);
        ResponseEntity<List<DocumentTaskDTO>> responseEntity = restTemplate
                .exchange( baseUrl + documentTasksUrl ,
                        HttpMethod.GET,
                        entity,
                        new ParameterizedTypeReference<List<DocumentTaskDTO>>(){});
        testContext.getHttpContext().setResponseBodyAndStatesForResponse(responseEntity);
    }

    @Then("^the response code is (\\d+)$")
    public void the_response_code_is(int responseCode) {
        assertEquals("Response status code", responseCode, testContext.getHttpContext().getHttpResponseStatusCode());
    }

    @When("^POST document-task for for document with no annotations$")
    public void post_task_no_annotations() throws Exception {
        DocumentTaskDTO documentTaskDTO = new DocumentTaskDTO();
        documentTaskDTO.setInputDocumentId(UUID.randomUUID().toString());
        HttpEntity<DocumentTaskDTO> entity = new HttpEntity(header);
        ResponseEntity<List<DocumentTaskDTO>> responseEntity = restTemplate
                .exchange( baseUrl + documentTasksUrl ,
                        HttpMethod.POST,
                        entity,
                        new ParameterizedTypeReference<List<DocumentTaskDTO>>(){});
        testContext.getHttpContext().setResponseBodyAndStatesForResponse(responseEntity);
    }


}