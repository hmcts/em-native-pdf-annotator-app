package uk.gov.hmcts.reform.em.npa;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.google.common.collect.Maps;
import io.restassured.http.ContentType;
import lombok.extern.slf4j.Slf4j;
import net.serenitybdd.rest.SerenityRest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@ExtendWith(PactConsumerTestExt.class)
@ExtendWith(SpringExtension.class)
public class IdamConsumerTest {

    private static final String IDAM_DETAILS_URL = "/o/userinfo";
    private static final String IDAM_OPENID_TOKEN_URL = "/o/token";

    @Pact(provider = "Idam_api", consumer = "npa_api")
    public RequestResponsePact executeGetIdamAccessTokenAndGet200(PactDslWithProvider builder) throws JSONException {
        String[] rolesArray = new String[1];
        rolesArray[0] = "citizen";
        Map<String, String> requestheaders = Maps.newHashMap();
        requestheaders.put("Content-Type", "application/x-www-form-urlencoded");

        Map<String, String> responseheaders = Maps.newHashMap();
        responseheaders.put("Content-Type", "application/json");

        Map<String, Object> params = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        params.put("email", "emCaseOfficer@email.net");
        params.put("password", "Password123");
        params.put("forename", "emCaseOfficer");
        params.put("surname", "jar123");
        params.put("roles", rolesArray);

        return builder
                .given("a user exists", params)
                .uponReceiving("Provider takes user/pwd and returns Access Token to Native PDF Annotator API")
                .path(IDAM_OPENID_TOKEN_URL)
                .method(HttpMethod.POST.toString())
                .body("redirect_uri=http%3A%2F%2Fwww.dummy-pact-service.com%2Fcallback&client_id=pact"
                                + "&grant_type=password&username=emCaseOfficer%40email.net&password=Password123"
                                + "&client_secret=pactsecret&scope=openid profile roles",
                        "application/x-www-form-urlencoded")
                .willRespondWith()
                .status(HttpStatus.OK.value())
                .headers(responseheaders)
                .body(createAuthResponse())
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "executeGetIdamAccessTokenAndGet200")
    public void should_post_to_token_endpoint_and_receive_access_token_with_200_response(MockServer mockServer)
            throws JSONException {
        String actualResponseBody =
                SerenityRest
                        .given()
                        .contentType(ContentType.URLENC)
                        .formParam("redirect_uri", "http://www.dummy-pact-service.com/callback")
                        .formParam("client_id", "pact")
                        .formParam("grant_type", "password")
                        .formParam("username", "emCaseOfficer@email.net")
                        .formParam("password", "Password123")
                        .formParam("client_secret", "pactsecret")
                        .formParam("scope", "openid profile roles")
                        .post(mockServer.getUrl() + IDAM_OPENID_TOKEN_URL)
                        .then()
                        .log().all().extract().asString();

        JSONObject response = new JSONObject(actualResponseBody);

        assertThat(response).isNotNull();
        assertThat(response.getString("access_token")).isNotBlank();

    }

    @Pact(provider = "Idam_api", consumer = "npa_api")
    public RequestResponsePact executeGetUserDetailsAndGet200(PactDslWithProvider builder) {

        Map<String, String> requestHeaders = Maps.newHashMap();
        requestHeaders.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> params = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        params.put("redirect_uri", "http://www.dummy-pact-service.com/callback");
        params.put("client_id", "pact");
        params.put("client_secret", "pactsecret");
        params.put("scope", "openid profile roles");
        params.put("username", "emCaseOfficer@email.net");
        params.put("password", "Password123");

        Map<String, String> responseheaders = Maps.newHashMap();
        responseheaders.put("Content-Type", "application/json");

        return builder
                .given("I have obtained an access_token as a user", params)
                .uponReceiving("Provider returns user info to Native PDF Annotator API")
                .path(IDAM_DETAILS_URL)
                .headers("Authorization", "Bearer eyJ0eXAiOiJKV1QiLCJraWQiOiJiL082T3ZWdjEre")
                .method(HttpMethod.GET.toString())
                .willRespondWith()
                .status(HttpStatus.OK.value())
                .headers(responseheaders)
                .body(createUserInfoResponse())
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "executeGetUserDetailsAndGet200")
    public void should_get_user_details_with_access_token(MockServer mockServer) throws JSONException {

        Map<String, String> headers = Maps.newHashMap();
        headers.put(HttpHeaders.AUTHORIZATION, "Bearer eyJ0eXAiOiJKV1QiLCJraWQiOiJiL082T3ZWdjEre");
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        String detailsResponseBody =
                SerenityRest
                        .given()
                        .headers(headers)
                        .when()
                        .get(mockServer.getUrl() + IDAM_DETAILS_URL)
                        .then()
                        .statusCode(200)
                        .and()
                        .extract()
                        .body()
                        .asString();

        JSONObject response = new JSONObject(detailsResponseBody);

        assertThat(detailsResponseBody).isNotNull();
        assertThat(response).hasNoNullFieldsOrProperties();
        assertThat(response.getString("uid")).isNotBlank();
        assertThat(response.getString("given_name")).isNotBlank();
        assertThat(response.getString("family_name")).isNotBlank();
        JSONArray rolesArr = response.getJSONArray("roles");
        assertThat(rolesArr).isNotNull();
        assertThat(rolesArr.length()).isNotZero();
        assertThat(rolesArr.get(0).toString()).isNotBlank();

    }

    private DslPart createUserInfoResponse() {

        return new PactDslJsonBody()
                .stringType("uid", "1234-2345-3456-4567")
                .stringType("given_name", "emCaseOfficer")
                .stringType("family_name", "Jar")
                .array("roles")
                .stringType("citizen")
                .closeArray();

    }

    private PactDslJsonBody createAuthResponse() {

        return new PactDslJsonBody()
                .stringType("access_token", "eyJ0eXAiOiJKV1QiLCJraWQiOiJiL082T3ZWdjEre")
                .stringType("refresh_token", "eyJ0eXAiOiJKV1QiLCJ6aXAiOiJOT05FIiwia2lkIjoiYi9PNk92V")
                .stringType("scope", "openid roles profile")
                .stringType("id_token", "eyJ0eXAiOiJKV1QiLCJraWQiOiJiL082T3ZWdjEre")
                .stringType("token_type", "Bearer")
                .stringType("expires_in", "28798");
    }

}
