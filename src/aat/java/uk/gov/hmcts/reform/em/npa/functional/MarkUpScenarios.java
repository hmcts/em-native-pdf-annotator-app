package uk.gov.hmcts.reform.em.npa.functional;

import io.restassured.specification.RequestSpecification;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import net.thucydides.core.annotations.WithTag;
import net.thucydides.core.annotations.WithTags;
import org.json.JSONObject;
import org.junit.Assert;
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
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RectangleDTO;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RedactionDTO;
import uk.gov.hmcts.reform.em.npa.testutil.TestUtil;
import uk.gov.hmcts.reform.em.test.retry.RetryRule;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@SpringBootTest(classes = {TestUtil.class, EmTestConfig.class})
@TestPropertySource(value = "classpath:application.yml")
@RunWith(SpringIntegrationSerenityRunner.class)
@WithTags({@WithTag("testType:Functional")})
public class MarkUpScenarios {

    @Autowired
    private TestUtil testUtil;

    @Value("${test.url}")
    private String testUrl;

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

    @Test
    public void testCreateMarkUp() {

        RedactionDTO redactionDTO = testUtil.createRedactionDTO(docId, redactionId);

        JSONObject jsonObject = new JSONObject(redactionDTO);

        RedactionDTO response =
                request
                        .body(jsonObject)
                        .post("/api/markups")
                        .then()
                        .statusCode(201)
                        .extract()
                        .body()
                        .as(RedactionDTO.class);


        Assert.assertEquals(redactionDTO.getDocumentId(), response.getDocumentId());
        Assert.assertEquals(redactionDTO.getRedactionId(), response.getRedactionId());
        Assert.assertEquals(redactionDTO.getRectangles().size(), response.getRectangles().size());
    }

    @Test
    public void testGetAllDocumentMarkUps() {

        // First create Test data
        RedactionDTO redactionDTO = testUtil.createRedactionDTO(docId, redactionId);

        JSONObject jsonObject = new JSONObject(redactionDTO);

        request
                .body(jsonObject)
                .post("/api/markups")
                .then()
                .statusCode(201);

        //Now test the GET using the above created Data
        Map<String, Integer> params = new HashMap<>();
        params.put("page", 0);
        params.put("size", 10);

        List<RedactionDTO> response =
                request
                        .params(params)
                        .get("/api/markups/" + docId)
                        .then()
                        .statusCode(200)
                        .extract()
                        .response()
                        .jsonPath()
                        .getList(".", RedactionDTO.class);

        Assert.assertNotNull(response);
        Assert.assertEquals(1, response.size());
        RedactionDTO responseDto = response.get(0);

        Assert.assertEquals(redactionDTO.getDocumentId(), responseDto.getDocumentId());
        Assert.assertEquals(redactionDTO.getRedactionId(), responseDto.getRedactionId());
        Assert.assertEquals(redactionDTO.getRectangles().size(), responseDto.getRectangles().size());

    }

    @Test
    public void testUpdateMarkUp() {

        RedactionDTO redactionDTO = testUtil.createRedactionDTO(docId, redactionId);
        RectangleDTO rectangleDTO = redactionDTO.getRectangles().stream().findFirst().get();
        rectangleDTO.setHeight(100.0);
        rectangleDTO.setWidth(60.0);

        JSONObject jsonObject = new JSONObject(redactionDTO);

        RedactionDTO response =
                request
                        .body(jsonObject)
                        .put("/api/markups")
                        .then()
                        .statusCode(200)
                        .extract()
                        .body()
                        .as(RedactionDTO.class);

        RectangleDTO resRectangleDTO = response.getRectangles().stream().findFirst().get();
        Assert.assertEquals(redactionDTO.getDocumentId(), response.getDocumentId());
        Assert.assertEquals(redactionDTO.getRedactionId(), response.getRedactionId());
        Assert.assertEquals(Double.valueOf(100.0), resRectangleDTO.getHeight());
        Assert.assertEquals(Double.valueOf(60.0), resRectangleDTO.getWidth());

    }

    @Test
    public void testDeleteMarkUp() {
        request
                .delete("/api/markups/" + docId)
                .then()
                .statusCode(200);
    }
}
