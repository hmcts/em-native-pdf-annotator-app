package uk.gov.hmcts.reform.em.npa.consumer;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.LambdaDsl;
import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import lombok.extern.slf4j.Slf4j;
import net.serenitybdd.rest.SerenityRest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Slf4j
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(pactVersion = PactSpecVersion.V3)
@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class NpaPactRedactionConsumerTest {

    public static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    public static final String AUTH_TOKEN = "Bearer someAuthorizationToken";
    public static final String SERVICE_AUTH_TOKEN = "Bearer someServiceAuthorizationToken";

    private static final UUID REDACTION_ID = UUID.fromString("4c34ba4a-585a-407d-aa78-3f86f3171cdd");
    private static final UUID DOCUMENT_ID = UUID.fromString("f2cc4d79-d0f3-4b43-affe-535516370cdd");

    public Map<String, String> getHeaders() {
        return Map.of(
                SERVICE_AUTHORIZATION, SERVICE_AUTH_TOKEN,
                AUTHORIZATION, AUTH_TOKEN,
                "Content-Type", "application/json"
        );
    }

    @Pact(consumer = "em_npa_redaction_api", provider = "em_npa_redaction_api")
    public V4Pact burnMarkupsSuccessPact(PactBuilder builder) {
        return builder
                .usingLegacyDsl()
                .given("Valid redaction request exists")
                .uponReceiving("POST request to burn markups")
                .path("/api/redaction")
                .method("POST")
                .headers(getHeaders())
                .body(LambdaDsl.newJsonBody(root -> {
                    root.stringType("caseId", "123456789");
                    root.uuid("documentId", DOCUMENT_ID);
                    root.stringType("redactedFileName", "document-redacted.pdf");
                    root.minArrayLike("redactions", 1, redaction -> {
                        redaction.uuid("redactionId", REDACTION_ID);
                        redaction.uuid("documentId", DOCUMENT_ID);
                        redaction.integerType("page", 1);
                    });
                }).build())
                .willRespondWith()
                .status(200)
                .headers(Map.of(
                        "Content-Disposition", "attachment; filename=\"document-redacted.pdf\"",
                        "Content-Type", "application/pdf"
                ))
                .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "burnMarkupsSuccessPact")
    void testBurnMarkupsSuccess(MockServer mockServer) {
        String requestBody = String.format("""
            {
              "caseId": "123456789",
              "documentId": "%s",
              "redactedFileName": "document-redacted.pdf",
              "redactions": [
                {
                  "redactionId": "%s",
                  "documentId": "%s",
                  "page": 1
                }
              ]
            }
            """, DOCUMENT_ID, REDACTION_ID, DOCUMENT_ID);

        SerenityRest
                .given()
                .headers(getHeaders())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(requestBody)
                .post(mockServer.getUrl() + "/api/redaction")
                .then()
                .statusCode(200)
                .header("Content-Disposition", "attachment; filename=\"document-redacted.pdf\"")
                .header("Content-Type", "application/pdf")
                .body(notNullValue());
    }
}
