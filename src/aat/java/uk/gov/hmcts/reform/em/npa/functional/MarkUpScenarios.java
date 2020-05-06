package uk.gov.hmcts.reform.em.npa.functional;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RectangleDTO;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RedactionDTO;
import uk.gov.hmcts.reform.em.npa.testutil.TestUtil;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@SpringBootTest(classes = {TestUtil.class, EmTestConfig.class})
@PropertySource(value = "classpath:application.yml")
@RunWith(SpringRunner.class)
public class MarkUpScenarios {

    @Autowired
    TestUtil testUtil;

    @Value("${test.url}")
    String testUrl;

    private static final UUID docId = UUID.randomUUID();
    private static final UUID redactionId = UUID.randomUUID();

    @Test
    public void testCreateMarkUp() throws JsonProcessingException {

        RedactionDTO redactionDTO = createRedactionDTO(docId, redactionId);

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
    public void testUpdateMarkUp() {

        RedactionDTO redactionDTO = createRedactionDTO(docId, redactionId);
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

    private RedactionDTO createRedactionDTO(UUID docId, UUID redactionId) {
        RedactionDTO redactionDTO = new RedactionDTO();
        redactionDTO.setDocumentId(docId);
        redactionDTO.setRedactionId(redactionId);
        redactionDTO.setPage(6);
        Set<RectangleDTO> rectangles = new HashSet<>();
        rectangles.add(createRectangleDTO());
        redactionDTO.setRectangles(rectangles);
        return redactionDTO;
    }

    private RectangleDTO createRectangleDTO() {
        RectangleDTO rectangleDTO = new RectangleDTO();
        rectangleDTO.setId(UUID.randomUUID());
        rectangleDTO.setHeight(10.0);
        rectangleDTO.setWidth(10.0);
        rectangleDTO.setX(20.0);
        rectangleDTO.setY(30.0);
        return rectangleDTO;
    }
}
