package uk.gov.hmcts.reform.em.npa.functional;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import net.thucydides.core.annotations.WithTag;
import net.thucydides.core.annotations.WithTags;
import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.em.npa.testutil.TestUtil;
import uk.gov.hmcts.reform.em.test.retry.RetryRule;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@SpringBootTest(classes = {TestUtil.class})
@TestPropertySource(value = "classpath:application.yml")
@RunWith(SpringIntegrationSerenityRunner.class)
@WithTags({@WithTag("testType:Functional")})
public class MarkUpScenarios {

    @Autowired
    private TestUtil testUtil;

    @Value("${test.url}")
    private String testUrl;

    @Value("${endpoint-toggles.search-markups}")
    private boolean searchMarkupsEnabled;

    @Rule
    public RetryRule retryRule = new RetryRule(3);

    private RequestSpecification request;
    private RequestSpecification unAuthenticatedRequest;

    @Before
    public void setupRequestSpecification() {
        request = testUtil
                .authRequest()
                .baseUri(testUrl)
                .contentType(APPLICATION_JSON_VALUE);

        unAuthenticatedRequest = testUtil
                .unauthenticatedRequest()
                .baseUri(testUrl)
                .contentType(APPLICATION_JSON_VALUE);
    }

    @Test
    public void shouldReturn201WhenCreateNewMarkUp() {
        final String redactionId = UUID.randomUUID().toString();
        final String documentId = UUID.randomUUID().toString();
        final String rectangleId = UUID.randomUUID().toString();
        final ValidatableResponse response = createMarkUp(redactionId, documentId, rectangleId);

        response
                .assertThat()
                .statusCode(201)
                .body("redactionId", equalTo(redactionId))
                .body("documentId", equalTo(documentId))
                .body("page", equalTo(1))
                .body("rectangles", Matchers.hasSize(1))
                .body("rectangles[0].x", equalTo(1f))
                .body("rectangles[0].y", equalTo(2f))
                .body("rectangles[0].width", equalTo(10f))
                .body("rectangles[0].height", equalTo(11f))
                .header("Location", equalTo("/api/markups/" + redactionId))
                .log().all();
    }

    @Test
    public void shouldReturn422WhenCreateNewMarkUpWithoutMandatoryFields() {
        final String redactionId = UUID.randomUUID().toString();
        final String documentId = UUID.randomUUID().toString();
        final String rectangleId = UUID.randomUUID().toString();
        final JSONObject jsonObject = testUtil.createMarkUpPayload(redactionId, documentId, rectangleId);

        jsonObject.remove("redactionId");
        jsonObject.remove("documentId");
        jsonObject.remove("page");

        request
                .body(jsonObject.toString())
                .post("/api/markups")
                .then()
                .assertThat()
                .statusCode(422)
                .body("type", equalTo("https://npa/problem/problem-with-message"))
                .body("title", equalTo("Unprocessable Entity"))
                .body("detail", notNullValue())
                .body("path", equalTo("/api/markups"))
                .body("message", equalTo("error.http.422"))
                .log().all();
    }

    @Test
    public void shouldReturn401WhenUnAuthenticatedUserCreateNewMarkUp() {
        final String redactionId = UUID.randomUUID().toString();
        final String documentId = UUID.randomUUID().toString();
        final String rectangleId = UUID.randomUUID().toString();
        final JSONObject jsonObject = testUtil.createMarkUpPayload(redactionId, documentId, rectangleId);

        unAuthenticatedRequest
                .body(jsonObject.toString())
                .post("/api/markups")
                .then()
                .assertThat()
                .statusCode(401);
    }

    @Test
    public void shouldReturn200WhenCreateSearchMarkUps() {
        Assume.assumeTrue(searchMarkupsEnabled);
        final String documentId = UUID.randomUUID().toString();
        final ValidatableResponse response = createSearchMarkUps(documentId);

        response
                .assertThat()
                .statusCode(200)
                .body("searchRedactions", Matchers.hasSize(3))
                .body("searchRedactions[0].documentId", equalTo(documentId))
                .body("searchRedactions[0].page", equalTo(1))
                .body("searchRedactions[0].rectangles", Matchers.hasSize(1))
                .body("searchRedactions[1].rectangles[0].x", equalTo(1f))
                .body("searchRedactions[1].rectangles[0].y", equalTo(2f))
                .body("searchRedactions[2].rectangles[0].width", equalTo(10f))
                .body("searchRedactions[2].rectangles[0].height", equalTo(11f))
                .log().all();
    }

    @Test
    public void shouldReturn422WhenCreateSearchMarkUpsWithoutMandatoryFields() {
        Assume.assumeTrue(searchMarkupsEnabled);
        final String documentId = UUID.randomUUID().toString();
        final JSONObject jsonObject = testUtil.createSearchMarkUpsPayload(documentId);

        jsonObject.remove("searchRedactions");

        request
                .body(jsonObject.toString())
                .post("/api/markups/search")
                .then()
                .assertThat()
                .statusCode(422)
                .body("type", equalTo("https://npa/problem/problem-with-message"))
                .body("title", equalTo("Unprocessable Entity"))
                .body("detail", notNullValue())
                .body("path", equalTo("/api/markups/search"))
                .body("message", equalTo("error.http.422"))
                .log().all();
    }

    @Test
    public void shouldReturn401WhenUnAuthenticatedUserCreateSearchMarkUps() {
        Assume.assumeTrue(searchMarkupsEnabled);
        final String documentId = UUID.randomUUID().toString();
        final JSONObject jsonObject = testUtil.createSearchMarkUpsPayload(documentId);

        unAuthenticatedRequest
                .body(jsonObject.toString())
                .post("/api/markups/search")
                .then()
                .assertThat()
                .statusCode(401);
    }


    @Test
    public void shouldReturn200WhenGetMarkUpByDocumentId() {
        final String redactionId = UUID.randomUUID().toString();
        final String documentId = UUID.randomUUID().toString();
        final String rectangleId = UUID.randomUUID().toString();
        final ValidatableResponse response = createMarkUp(redactionId, documentId, rectangleId);
        final String docId = extractJsonObjectFromResponse(response).getString("documentId");

        request
                .get("/api/markups/" + docId)
                .then()
                .assertThat()
                .statusCode(200)
                .body("size()", Matchers.greaterThanOrEqualTo(1))
                .body("[0].redactionId", equalTo(redactionId))
                .body("[0].documentId", equalTo(documentId))
                .body("[0].page", equalTo(1))
                .body("[0].rectangles", Matchers.hasSize(1))
                .body("[0].rectangles[0].id", equalTo(rectangleId))
                .body("[0].rectangles[0].x", equalTo(1f))
                .body("[0].rectangles[0].y", equalTo(2f))
                .body("[0].rectangles[0].width", equalTo(10f))
                .body("[0].rectangles[0].height", equalTo(11f))
                .log().all();
    }

    @Test
    public void shouldReturn404WhenGetMarkUpByNonExistentDocumentId() {
        final String documentId = UUID.randomUUID().toString();
        request
                .get("/api/markups/" + documentId)
                .then()
                .assertThat()
                .statusCode(404)
                .log().all();
    }

    @Test
    public void shouldReturn401WhenUnAuthenticatedUserGetMarkUpByDocumentId() {
        final String documentId = UUID.randomUUID().toString();

        unAuthenticatedRequest
                .get("/api/markups/" + documentId)
                .then()
                .assertThat()
                .statusCode(401)
                .log().all();
    }

    @Test
    public void shouldReturn200WhenUpdateMarkUp() {
        final String redactionId = UUID.randomUUID().toString();
        final String documentId = UUID.randomUUID().toString();
        final String rectangleId = UUID.randomUUID().toString();
        final ValidatableResponse response = createMarkUp(redactionId, documentId, rectangleId);

        final JSONObject jsonObject = extractJsonObjectFromResponse(response);
        final String newRedactionId = UUID.randomUUID().toString();
        final String newDocumentId = UUID.randomUUID().toString();

        jsonObject.put("redactionId", newRedactionId);
        jsonObject.put("documentId", newDocumentId);
        jsonObject.put("page", 2);

        request
                .body(jsonObject.toString())
                .put("/api/markups")
                .then()
                .assertThat()
                .statusCode(200)
                .body("redactionId", equalTo(newRedactionId))
                .body("documentId", equalTo(newDocumentId))
                .body("page", equalTo(2))
                .body("rectangles", Matchers.hasSize(1))
                .body("rectangles[0].x", equalTo(1f))
                .body("rectangles[0].y", equalTo(2f))
                .body("rectangles[0].width", equalTo(10f))
                .body("rectangles[0].height", equalTo(11f))
                .log().all();
    }

    @Test
    public void shouldReturn422WhenUpdateMarkUpWithoutMandatoryFields() {
        final String redactionId = UUID.randomUUID().toString();
        final String documentId = UUID.randomUUID().toString();
        final String rectangleId = UUID.randomUUID().toString();
        final ValidatableResponse response = createMarkUp(redactionId, documentId, rectangleId);
        final JSONObject jsonObject = extractJsonObjectFromResponse(response);

        jsonObject.remove("redactionId");
        jsonObject.remove("documentId");
        jsonObject.remove("page");

        request
                .body(jsonObject.toString())
                .put("/api/markups")
                .then()
                .assertThat()
                .statusCode(422)
                .body("type", equalTo("https://npa/problem/problem-with-message"))
                .body("title", equalTo("Unprocessable Entity"))
                .body("detail", notNullValue())
                .body("path", equalTo("/api/markups"))
                .body("message", equalTo("error.http.422"))
                .log().all();
    }

    @Test
    public void shouldReturn401WhenUnAuthenticatedUserUpdateMarkUp() {
        final String redactionId = UUID.randomUUID().toString();
        final String documentId = UUID.randomUUID().toString();
        final String rectangleId = UUID.randomUUID().toString();
        final ValidatableResponse response = createMarkUp(redactionId, documentId, rectangleId);

        final JSONObject jsonObject = extractJsonObjectFromResponse(response);
        final String newRedactionId = UUID.randomUUID().toString();
        final String newDocumentId = UUID.randomUUID().toString();

        jsonObject.put("redactionId", newRedactionId);
        jsonObject.put("documentId", newDocumentId);
        jsonObject.put("page", 2);

        unAuthenticatedRequest
                .body(jsonObject.toString())
                .put("/api/markups")
                .then()
                .assertThat()
                .statusCode(401)
                .log().all();
    }

    @Test
    public void shouldReturn204WhenDeleteMarkUpByDocumentId() {
        final String redactionId = UUID.randomUUID().toString();
        final String documentId = UUID.randomUUID().toString();
        final String rectangleId = UUID.randomUUID().toString();
        final ValidatableResponse response = createMarkUp(redactionId, documentId, rectangleId);
        final String docId = extractJsonObjectFromResponse(response).getString("documentId");
        final ValidatableResponse deletedResponse = deleteMarkUpByDocumentId(docId);

        deletedResponse
                .assertThat()
                .statusCode(200) //FIXME: it should be 204
                .log().all();
    }

    @Test
    public void shouldReturn404WhenDeleteMarkUpByNonExistentDocumentId() {
        final String nonExistentDocumentId = UUID.randomUUID().toString();
        final ValidatableResponse deletedResponse = deleteMarkUpByDocumentId(nonExistentDocumentId);

        deletedResponse
                .assertThat()
                .statusCode(200) //FIXME: it should be 404
                .log().all();
    }

    @Test
    public void shouldReturn401WhenUnAuthenticatedUserDeleteMarkUpByDocumentId() {
        final String redactionId = UUID.randomUUID().toString();
        final String documentId = UUID.randomUUID().toString();
        final String rectangleId = UUID.randomUUID().toString();
        final ValidatableResponse response = createMarkUp(redactionId, documentId, rectangleId);
        final String docId = extractJsonObjectFromResponse(response).getString("documentId");

        unAuthenticatedRequest
                .delete("/api/markups/" + docId)
                .then()
                .assertThat()
                .statusCode(401)
                .log().all();
    }

    @Test
    public void shouldReturn204WhenDeleteMarkUpByDocumentIdAndRedactionId() {
        final String redactionId = UUID.randomUUID().toString();
        final String documentId = UUID.randomUUID().toString();
        final String rectangleId = UUID.randomUUID().toString();
        final ValidatableResponse response = createMarkUp(redactionId, documentId, rectangleId);
        final JSONObject jsonObject = extractJsonObjectFromResponse(response);
        final String docId = jsonObject.getString("documentId");
        final String redactId = jsonObject.getString("redactionId");
        final ValidatableResponse deletedResponse = deleteMarkUpByDocumentIdAndRedactionId(docId, redactId);

        deletedResponse
                .assertThat()
                .statusCode(200) //FIXME: it should be 204
                .log().all();
    }

    @Test
    public void shouldReturn404WhenDeleteMarkUpByNonExistentRedactionId() {
        final String nonExistentRedactionId = UUID.randomUUID().toString();
        final String documentId = UUID.randomUUID().toString();
        final ValidatableResponse deletedResponse =
            deleteMarkUpByDocumentIdAndRedactionId(documentId, nonExistentRedactionId);

        deletedResponse
                .statusCode(200) //FIXME: it should be 404
                .log().all();
    }

    @Test
    public void shouldReturn401WhenUnAuthenticatedUserDeleteMarkUpByDocumentIdAndRedactionId() {
        final String redactionId = UUID.randomUUID().toString();
        final String documentId = UUID.randomUUID().toString();
        final String rectangleId = UUID.randomUUID().toString();
        final ValidatableResponse response = createMarkUp(redactionId, documentId, rectangleId);
        final JSONObject jsonObject = extractJsonObjectFromResponse(response);
        final String docId = jsonObject.getString("documentId");
        final String redactId = jsonObject.getString("redactionId");

        unAuthenticatedRequest
                .delete(String.format("/api/markups/%s/%s", docId, redactId))
                .then()
                .assertThat()
                .statusCode(401)
                .log().all();
    }

    @Test
    public void shouldReturn200WhenGetAllMarkupsMoreThan20ByDocumentId() {
        List<String> redactions = new ArrayList<>();
        final UUID documentId = UUID.randomUUID();

        for (int i = 0; i < 30; i++) {
            final UUID redactionId = UUID.randomUUID();
            final UUID rectangleId = UUID.randomUUID();
            final JSONObject jsonObject = testUtil.createMarkUpPayload(
                    redactionId.toString(),documentId.toString(),rectangleId.toString());

            final ValidatableResponse response =
                    request.log().all()
                            .body(jsonObject.toString())
                            .post("/api/markups")
                            .then()
                            .statusCode(201);
            redactions.add(redactionId.toString());
        }

        request
                .get(String.format("/api/markups/%s", documentId))
                .then()
                .statusCode(200)
                .body("redactionId", containsInAnyOrder(redactions.toArray()))
                .log().all();
    }

    @NotNull
    private ValidatableResponse createMarkUp(final String redactionId, final String documentId,
                                             final String rectangleId) {
        final JSONObject jsonObject = testUtil.createMarkUpPayload(redactionId, documentId, rectangleId);

        return request
                .body(jsonObject.toString())
                .post("/api/markups")
                .then()
                .assertThat()
                .statusCode(201);
    }

    @NotNull
    private ValidatableResponse createSearchMarkUps(final String documentId) {
        final JSONObject jsonObject = testUtil.createSearchMarkUpsPayload(documentId);

        return request
                .body(jsonObject.toString())
                .post("/api/markups/search")
                .then()
                .assertThat()
                .statusCode(200);
    }

    @NotNull
    private ValidatableResponse deleteMarkUpByDocumentId(final String documentId) {
        return request
                .delete("/api/markups/" + documentId)
                .then()
                .log().all();
    }

    @NotNull
    private ValidatableResponse deleteMarkUpByDocumentIdAndRedactionId(final String documentId,
                                                                       final String redactionId) {
        return request
                .delete(String.format("/api/markups/%s/%s", documentId, redactionId))
                .then()
                .log().all();
    }

    @NotNull
    private JSONObject extractJsonObjectFromResponse(final ValidatableResponse response) {
        return response.extract().response().as(JSONObject.class);
    }
}
