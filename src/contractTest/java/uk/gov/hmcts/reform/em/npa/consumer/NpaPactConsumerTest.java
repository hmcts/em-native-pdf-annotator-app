package uk.gov.hmcts.reform.em.npa.consumer;


import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.LambdaDsl;
import au.com.dius.pact.consumer.dsl.PactBuilder;
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

import static au.com.dius.pact.consumer.dsl.LambdaDsl.newJsonArray;
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
    private static final UUID REDACTION_ID = UUID.fromString("4c34ba4a-585a-407d-aa78-3f86f3171cdd");
    private static final UUID DOCUMENT_ID = UUID.fromString("f2cc4d79-d0f3-4b43-affe-535516370cdd");
    private static final UUID RECTANGLE_ID = UUID.fromString("c04b807f-8352-4bfc-95b5-cecd072b7aba");

    public Map<String, String> getHeaders() {
        return Map.of(
                SERVICE_AUTHORIZATION, SERVICE_AUTH_TOKEN,
                AUTHORIZATION, AUTH_TOKEN,
                "Content-Type", "application/json"
        );
    }

    @Pact(consumer = "em_npa_api", provider = "native_pdf_annotator_api_provider")
    public V4Pact createMarkUpPact(PactBuilder builder) {
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

    @Pact(consumer = "em_npa_api", provider = "native_pdf_annotator_api_provider")
    public V4Pact getMarkupsPact(PactBuilder builder) {
        return builder
                .usingLegacyDsl()
                .given("Markups exist for document " + DOCUMENT_ID)
                .uponReceiving("GET request for markups by document ID")
                .path("/api/markups/" + DOCUMENT_ID)
                .method("GET")
                .headers(getHeaders())
                .willRespondWith()
                .status(200)
                .headers(Map.of("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .body(newJsonArray(array ->
                        array.object(markup -> {
                            markup.uuid("redactionId", REDACTION_ID);
                            markup.uuid("documentId", DOCUMENT_ID);
                            markup.integerType("page", 1);
                            markup.array("rectangles", rectangles ->
                                    rectangles.object(rect -> {
                                        rect.uuid("id", RECTANGLE_ID);
                                        rect.numberType("x", 10.5);
                                        rect.numberType("y", 20.5);
                                        rect.numberType("width", 100.0);
                                        rect.numberType("height", 200.0);
                                    })
                            );
                        }).build()
                ).build())
                .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "getMarkupsPact")
    void testGetMarkupsByDocumentId(MockServer mockServer) {
        SerenityRest
                .given()
                .headers(getHeaders())
                .get(mockServer.getUrl() + "/api/markups/" + DOCUMENT_ID)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("[0].redactionId", equalTo(REDACTION_ID.toString()))
                .body("[0].documentId", equalTo(DOCUMENT_ID.toString()))
                .body("[0].rectangles[0].id", equalTo(RECTANGLE_ID.toString()))
                .body("[0].rectangles[0].x", equalTo(10.5f));
    }

    @Pact(consumer = "em_npa_api", provider = "native_pdf_annotator_api_provider")
    public V4Pact updateMarkUpPact(PactBuilder builder) {
        return builder
                .usingLegacyDsl()
                .given("A valid RedactionDTO with ID exists")
                .uponReceiving("PUT request to update RedactionDTO")
                .path("/api/markups")
                .method("PUT")
                .headers(getHeaders())
                .body(LambdaDsl.newJsonBody(root -> {
                    root.uuid("redactionId", REDACTION_ID);
                    root.uuid("documentId", DOCUMENT_ID);
                    root.integerType("page", 1);
                    root.minArrayLike("rectangles", 1, rect -> {
                        rect.uuid("id", RECTANGLE_ID);
                        rect.numberType("x", 10.5);
                        rect.numberType("y", 20.5);
                        rect.numberType("width", 100.0);
                        rect.numberType("height", 200.0);
                    });
                }).build())
                .willRespondWith()
                .status(200)
                .headers(Map.of("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .body(LambdaDsl.newJsonBody(root -> {
                    root.uuid("redactionId", REDACTION_ID);
                    root.uuid("documentId", DOCUMENT_ID);
                    root.integerType("page", 1);
                    root.minArrayLike("rectangles", 1, rect -> {
                        rect.uuid("id", RECTANGLE_ID);
                        rect.numberType("x", 10.5);
                        rect.numberType("y", 20.5);
                        rect.numberType("width", 100.0);
                        rect.numberType("height", 200.0);
                    });
                }).build())
                .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "updateMarkUpPact")
    void testUpdateMarkUp(MockServer mockServer) {
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
                .put(mockServer.getUrl() + "/api/markups")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("redactionId", equalTo(REDACTION_ID.toString()))
                .body("documentId", equalTo(DOCUMENT_ID.toString()))
                .body("rectangles[0].id", equalTo(RECTANGLE_ID.toString()))
                .body("rectangles[0].x", equalTo(10.5f));
    }


    @Pact(consumer = "em_npa_api", provider = "native_pdf_annotator_api_provider")
    public V4Pact deleteMarkupsPact(PactBuilder builder) {
        return builder
                .usingLegacyDsl()
                .given("Markups exist for document ID and can be deleted")
                .uponReceiving("DELETE request to remove all markups by document ID")
                .path("/api/markups/" + DOCUMENT_ID)
                .method("DELETE")
                .headers(getHeaders())
                .willRespondWith()
                .status(200)
                .matchHeader(
                        "X-npaApp-alert",
                        "A redaction is deleted with identifier " + DOCUMENT_ID)
                .matchHeader(
                        "X-npaApp-params",
                        DOCUMENT_ID.toString())
                .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "deleteMarkupsPact")
    void testDeleteMarkupsByDocumentId(MockServer mockServer) {
        SerenityRest
                .given()
                .headers(getHeaders())
                .delete(mockServer.getUrl() + "/api/markups/" + DOCUMENT_ID)
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @Pact(consumer = "em_npa_api", provider = "native_pdf_annotator_api_provider")
    public V4Pact deleteSingleMarkupPact(PactBuilder builder) {
        return builder
                .usingLegacyDsl()
                .given("A valid Redaction with document ID and redaction ID exists and can be deleted")
                .uponReceiving("DELETE request to remove specific markup by document and redaction ID")
                .path(String.format("/api/markups/%s/%s", DOCUMENT_ID, REDACTION_ID))
                .method("DELETE")
                .headers(getHeaders())
                .willRespondWith()
                .status(200)
                .matchHeader(
                        "X-npaApp-alert",
                        "A redaction is deleted with identifier " + REDACTION_ID)
                .matchHeader(
                        "X-npaApp-params",
                        REDACTION_ID.toString())
                .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "deleteSingleMarkupPact")
    void testDeleteSingleMarkup(MockServer mockServer) {
        SerenityRest
                .given()
                .headers(getHeaders())
                .delete(mockServer.getUrl()
                        + "/api/markups/f2cc4d79-d0f3-4b43-affe-535516370cdd/4c34ba4a-585a-407d-aa78-3f86f3171cdd")
                .then()
                .statusCode(200);
    }

}
