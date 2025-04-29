package uk.gov.hmcts.reform.em.npa;


import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.LambdaDsl;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import lombok.extern.slf4j.Slf4j;
import net.serenitybdd.rest.SerenityRest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Slf4j
@ExtendWith(PactConsumerTestExt.class)
@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class NpaPactConsumerTest {

    public static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    public static final String AUTH_TOKEN = "Bearer someAuthorizationToken";
    public static final String SERVICE_AUTH_TOKEN = "Bearer someServiceAuthorizationToken";

    // Static UUIDs from provided list
    private static final UUID REDACTION_ID = UUID.randomUUID();
    private static final UUID DOCUMENT_ID = UUID.randomUUID();
    private static final UUID RECTANGLE_ID = UUID.randomUUID();

    public Map<String, String> getHeaders() {
        return Map.of(
                SERVICE_AUTHORIZATION, SERVICE_AUTH_TOKEN,
                AUTHORIZATION, AUTH_TOKEN,
                "Content-Type", "application/json"
        );
    }

    @Pact(consumer = "em_npa_api", provider = "native_pdf_annotator_api_provider")
    public V4Pact createMarkUpPact(au.com.dius.pact.consumer.dsl.PactBuilder builder) {
        return builder
                .usingLegacyDsl()
                .given("A valid RedactionDTO exists")
                .uponReceiving("POST request to create RedactionDTO")
                .path("/api/markups")
                .method("POST")
                .headers(getHeaders())
                .body(LambdaDsl.newJsonBody(root -> {
                    root.uuid("redactionId", REDACTION_ID); // Set expected UUID
                    root.uuid("documentId", DOCUMENT_ID);   // Set expected UUID
                    root.integerType("page", 1);
                    root.minArrayLike("rectangles", 1, rect -> {
                        rect.uuid("id", RECTANGLE_ID); // Set expected UUID
                        rect.numberType("x", 10.5);
                        rect.numberType("y", 20.5);
                        rect.numberType("width", 100.0);
                        rect.numberType("height", 200.0);
                    });
                }).build())
                .willRespondWith()
                .status(201)
                .headers(Map.of("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .body(LambdaDsl.newJsonBody(root -> {
                    root.uuid("redactionId", REDACTION_ID); // Match request ID
                    root.uuid("documentId", DOCUMENT_ID);   // Match request ID
                    root.integerType("page", 1);
                    root.minArrayLike("rectangles", 1, rect -> {
                        rect.uuid("id", RECTANGLE_ID); // Match request ID
                        rect.numberType("x", 10.5);
                        rect.numberType("y", 20.5);
                        rect.numberType("width", 100.0);
                        rect.numberType("height", 200.0);
                    });
                }).build())
                .toPact(V4Pact.class);
    }


    @Test
    @PactTestFor(pactMethod = "createMarkUpPact")
    void testCreateMarkUp(MockServer mockServer) {


        String requestBody = String.format("""
            {
              "redactionId": "%s",
              "documentId": "%s",
              "page": 1,
              "rectangles": [
                {
                  "id": "%s",
                  "x": 10.5,
                  "y": 20.5,
                  "width": 100.0,
                  "height": 200.0
                }
              ]
            }
            """, REDACTION_ID, DOCUMENT_ID, RECTANGLE_ID);

        SerenityRest
                .given()
                .headers(getHeaders())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(requestBody)
                .post(mockServer.getUrl() + "/api/markups")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("redactionId", equalTo(REDACTION_ID.toString()))  // Convert UUID to string
                .body("documentId", equalTo(DOCUMENT_ID.toString()))
                .body("rectangles[0].id", equalTo(RECTANGLE_ID.toString()));
    }
}
