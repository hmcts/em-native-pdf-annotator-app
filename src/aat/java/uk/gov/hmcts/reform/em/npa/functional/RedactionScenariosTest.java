package uk.gov.hmcts.reform.em.npa.functional;

import io.restassured.specification.RequestSpecification;
import net.serenitybdd.annotations.WithTag;
import net.serenitybdd.annotations.WithTags;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.em.EmTestConfig;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RedactionDTO;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RedactionRequest;
import uk.gov.hmcts.reform.em.npa.testutil.ExtendedCcdHelper;
import uk.gov.hmcts.reform.em.npa.testutil.TestUtil;
import uk.gov.hmcts.reform.em.npa.testutil.ToggleProperties;
import uk.gov.hmcts.reform.em.test.retry.RetryExtension;

import java.util.Arrays;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@SpringBootTest(classes = {TestUtil.class, EmTestConfig.class, ExtendedCcdHelper.class})
@TestPropertySource(value = "classpath:application.yml")
@ExtendWith({SerenityJUnit5Extension.class, SpringExtension.class})
@WithTags({@WithTag("testType:Functional")})
@EnableConfigurationProperties(ToggleProperties.class)
class RedactionScenariosTest {

    @Value("${test.url}")
    private String testUrl;

    @Autowired
    private TestUtil testUtil;

    @Autowired
    protected ExtendedCcdHelper extendedCcdHelper;

    @Autowired
    private ToggleProperties toggleProperties;

    @RegisterExtension
    RetryExtension retryExtension = new RetryExtension(3);

    private static final UUID documentId = UUID.randomUUID();
    private static final UUID redactionId = UUID.randomUUID();
    private RequestSpecification request;
    private RequestSpecification cdamRequest;

    @BeforeEach
    public void setupRequestSpecification() {
        request = testUtil
                .authRequest()
                .baseUri(testUrl)
                .contentType(APPLICATION_JSON_VALUE);

        cdamRequest = testUtil
            .cdamAuthRequest()
            .baseUri(testUrl)
            .contentType(APPLICATION_JSON_VALUE);

    }

    @Test
    void shouldReturn200WhenRedactedPdfDocument() {
        assumeFalse(toggleProperties.isCdamEnabled());
        final String newDocId = testUtil.uploadPdfDocumentAndReturnUrl();
        final RedactionRequest redactionRequest = new RedactionRequest();
        redactionRequest.setDocumentId(UUID.fromString(newDocId.substring(newDocId.lastIndexOf('/') + 1)));
        redactionRequest.setRedactions(Arrays.asList(createRedaction(), createRedaction()));

        final JSONObject jsonObject = new JSONObject(redactionRequest);

        request
                .body(jsonObject)
                .post("/api/redaction")
                .then()
                .assertThat()
                .statusCode(200)
                .body(notNullValue());
    }

    private RedactionDTO createRedaction() {
        final RedactionDTO redactionDTO = testUtil.createRedactionDTO(documentId, redactionId);
        redactionDTO.setPage(1);
        final JSONObject jsonObject = new JSONObject(redactionDTO);

        return request
                .body(jsonObject)
                .post("/api/markups")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .as(RedactionDTO.class);
    }
}
