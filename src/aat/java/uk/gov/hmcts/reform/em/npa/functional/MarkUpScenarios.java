package uk.gov.hmcts.reform.em.npa.functional;

import com.fasterxml.jackson.core.JsonProcessingException;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.em.EmTestConfig;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RectangleDTO;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RedactionDTO;
import uk.gov.hmcts.reform.em.npa.testutil.TestUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SpringBootTest(classes = {TestUtil.class, EmTestConfig.class})
@PropertySource(value = "classpath:application.yml")
@RunWith(SpringIntegrationSerenityRunner.class)
public class MarkUpScenarios {

    @Autowired
    TestUtil testUtil;

    @Value("${test.url}")
    String testUrl;

    private static final UUID docId = UUID.randomUUID();
    private static final UUID redactionId = UUID.randomUUID();

    @Test
    public void testCreateMarkUp() throws JsonProcessingException {

        RedactionDTO redactionDTO = testUtil.createRedactionDTO(docId, redactionId);

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


        Assert.assertEquals(redactionDTO.getDocumentId(), response.getDocumentId());
        Assert.assertEquals(redactionDTO.getRedactionId(), response.getRedactionId());
        Assert.assertEquals(redactionDTO.getRectangles().size(), response.getRectangles().size());
    }

    @Test
    public void testGetAllDocumentMarkUps() {

        // First create Test data
        RedactionDTO redactionDTO = testUtil.createRedactionDTO(docId, redactionId);

        JSONObject jsonObject = new JSONObject(redactionDTO);

        testUtil.authRequest()
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .body(jsonObject)
            .request("POST", testUrl + "/api/markups")
            .then()
            .statusCode(201);

        //Now test the GET using the above created Data
        Map<String, Integer> params = new HashMap<>();
        params.put("page", 0);
        params.put("size", 10);

        List<RedactionDTO> response =  testUtil.authRequest()
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .params(params)
            .request("GET", testUrl + "/api/markups/"+docId)
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

        RedactionDTO response = testUtil.authRequest()
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .body(jsonObject)
            .request("PUT", testUrl + "/api/markups")
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

        testUtil.authRequest()
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .request("DELETE", testUrl + "/api/markups/" + docId)
            .then()
            .statusCode(200);
    }
}
