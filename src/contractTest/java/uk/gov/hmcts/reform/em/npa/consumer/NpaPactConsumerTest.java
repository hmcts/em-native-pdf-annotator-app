package uk.gov.hmcts.reform.em.npa.consumer;


import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.LambdaDsl;
import au.com.dius.pact.consumer.dsl.LambdaDslObject;
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
// S1192: Using string literals for JSON/request field names intentionally to keep structure clear in tests.
@SuppressWarnings("squid:S1192")
public class NpaPactConsumerTest {

    public static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    public static final String AUTH_TOKEN = "Bearer someAuthorizationToken";
    public static final String SERVICE_AUTH_TOKEN = "Bearer someServiceAuthorizationToken";

    private static final String NPA_PROVIDER = "native_pdf_annotator_api_provider";
    private static final String MARKUPS_API_PATH = "/api/markups";

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

    @Pact(consumer = "em_npa_api", provider = NPA_PROVIDER)
    public V4Pact createMarkUpPact(PactBuilder builder) {
        return builder
            .usingLegacyDsl()
            .given("A valid RedactionDTO exists")
            .uponReceiving("POST request to create RedactionDTO")
            .path(MARKUPS_API_PATH)
            .method("POST")
            .headers(getHeaders())
            .body(createRedactionDtoDsl())
            .willRespondWith()
            .status(HttpStatus.CREATED.value())
            .headers(Map.of("Content-Type", MediaType.APPLICATION_JSON_VALUE))
            .body(createRedactionDtoDsl())
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "createMarkUpPact")
    void testCreateMarkUp(MockServer mockServer) {
        SerenityRest
            .given()
            .headers(getHeaders())
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(createRedactionDtoDsl().getBody().toString())
            .post(mockServer.getUrl() + MARKUPS_API_PATH)
            .then()
            .statusCode(HttpStatus.CREATED.value())
            .body("redactionId", equalTo(REDACTION_ID.toString()))
            .body("documentId", equalTo(DOCUMENT_ID.toString()))
            .body("rectangles[0].id", equalTo(RECTANGLE_ID.toString()));
    }

    @Pact(consumer = "em_npa_api", provider = NPA_PROVIDER)
    public V4Pact getMarkupsPact(PactBuilder builder) {
        return builder
            .usingLegacyDsl()
            .given("Markups exist for document " + DOCUMENT_ID)
            .uponReceiving("GET request for markups by document ID")
            .path(MARKUPS_API_PATH + "/" + DOCUMENT_ID)
            .method("GET")
            .headers(getHeaders())
            .willRespondWith()
            .status(HttpStatus.OK.value())
            .headers(Map.of("Content-Type", MediaType.APPLICATION_JSON_VALUE))
            .body(createRedactionDtoArrayDsl())
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "getMarkupsPact")
    void testGetMarkupsByDocumentId(MockServer mockServer) {
        SerenityRest
            .given()
            .headers(getHeaders())
            .get(mockServer.getUrl() + MARKUPS_API_PATH + "/" + DOCUMENT_ID)
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("[0].redactionId", equalTo(REDACTION_ID.toString()))
            .body("[0].documentId", equalTo(DOCUMENT_ID.toString()))
            .body("[0].rectangles[0].id", equalTo(RECTANGLE_ID.toString()))
            .body("[0].rectangles[0].x", equalTo(10.5f));
    }

    @Pact(consumer = "em_npa_api", provider = NPA_PROVIDER)
    public V4Pact updateMarkUpPact(PactBuilder builder) {
        return builder
            .usingLegacyDsl()
            .given("A valid RedactionDTO with ID exists")
            .uponReceiving("PUT request to update RedactionDTO")
            .path(MARKUPS_API_PATH)
            .method("PUT")
            .headers(getHeaders())
            .body(createRedactionDtoDsl())
            .willRespondWith()
            .status(HttpStatus.OK.value())
            .headers(Map.of("Content-Type", MediaType.APPLICATION_JSON_VALUE))
            .body(createRedactionDtoDsl())
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "updateMarkUpPact")
    void testUpdateMarkUp(MockServer mockServer) {
        SerenityRest
            .given()
            .headers(getHeaders())
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(createRedactionDtoDsl().getBody().toString())
            .put(mockServer.getUrl() + MARKUPS_API_PATH)
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("redactionId", equalTo(REDACTION_ID.toString()))
            .body("documentId", equalTo(DOCUMENT_ID.toString()))
            .body("rectangles[0].id", equalTo(RECTANGLE_ID.toString()))
            .body("rectangles[0].x", equalTo(10.5f));
    }

    @Pact(consumer = "em_npa_api", provider = NPA_PROVIDER)
    public V4Pact deleteMarkupsPact(PactBuilder builder) {
        return builder
            .usingLegacyDsl()
            .given("Markups exist for document ID and can be deleted")
            .uponReceiving("DELETE request to remove all markups by document ID")
            .path(MARKUPS_API_PATH + "/" + DOCUMENT_ID)
            .method("DELETE")
            .headers(getHeaders())
            .willRespondWith()
            .status(HttpStatus.OK.value())
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
            .delete(mockServer.getUrl() + MARKUPS_API_PATH + "/" + DOCUMENT_ID)
            .then()
            .statusCode(HttpStatus.OK.value());
    }

    @Pact(consumer = "em_npa_api", provider = NPA_PROVIDER)
    public V4Pact deleteSingleMarkupPact(PactBuilder builder) {
        String path = String.format("%s/%s/%s", MARKUPS_API_PATH, DOCUMENT_ID, REDACTION_ID);
        return builder
            .usingLegacyDsl()
            .given("A valid Redaction with document ID and redaction ID exists and can be deleted")
            .uponReceiving("DELETE request to remove specific markup by document and redaction ID")
            .path(path)
            .method("DELETE")
            .headers(getHeaders())
            .willRespondWith()
            .status(HttpStatus.OK.value())
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
        String path = String.format("%s/%s/%s", MARKUPS_API_PATH, DOCUMENT_ID, REDACTION_ID);
        SerenityRest
            .given()
            .headers(getHeaders())
            .delete(mockServer.getUrl() + path)
            .then()
            .statusCode(HttpStatus.OK.value());
    }

    private void buildRedactionDto(LambdaDslObject root) {
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
    }

    private DslPart createRedactionDtoDsl() {
        return LambdaDsl.newJsonBody(this::buildRedactionDto).build();
    }

    private DslPart createRedactionDtoArrayDsl() {
        return newJsonArray(array -> array.object(this::buildRedactionDto)).build();
    }

}