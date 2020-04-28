package uk.gov.hmcts.reform.em.npa.functional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import uk.gov.hmcts.reform.em.npa.testutil.TestUtil;

import java.util.UUID;

@SpringBootTest(classes = {TestUtil.class, EmTestConfig.class})
@PropertySource(value = "classpath:application.yml")
@RunWith(SpringRunner.class)
public class MarkUpScenarios {

    @Autowired
    TestUtil testUtil;

    @Value("${test.url}")
    String testUrl;

    private static final UUID id = UUID.randomUUID();

    @Test
    public void testCreateMarkUp() throws JsonProcessingException {

        MarkUpDTO markUpDTO = new MarkUpDTO();
        markUpDTO.setDocumentId(id);
        markUpDTO.setId(id);
        markUpDTO.setHeight(10);
        markUpDTO.setWidth(10);
        markUpDTO.setXcoordinate(20);
        markUpDTO.setYcoordinate(30);

        JSONObject jsonObject = new JSONObject(markUpDTO);

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(markUpDTO);

        testUtil.authRequest()
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .body(json)
            .request("POST", testUrl + "/api/markups")
            .then()
            .statusCode(201);

    }

    @Test
    public void testUpdateMarkUp() {

        MarkUpDTO markUpDTO = new MarkUpDTO();
        markUpDTO.setDocumentId(id);
        markUpDTO.setId(UUID.randomUUID());
        markUpDTO.setHeight(10);
        markUpDTO.setWidth(10);
        markUpDTO.setXcoordinate(20);
        markUpDTO.setYcoordinate(30);

        JSONObject jsonObject = new JSONObject(markUpDTO);

        testUtil.authRequest()
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .body(jsonObject)
            .request("PUT", testUrl + "/api/markups")
            .then()
            .statusCode(200);

    }

    @Test
    public void testDeleteMarkUp() {

        testUtil.authRequest()
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .request("DELETE", testUrl + "/api/markups/" + id)
            .then()
            .statusCode(200);
    }
}
