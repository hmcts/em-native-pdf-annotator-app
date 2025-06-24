package uk.gov.hmcts.reform.em.npa.consumer;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import net.serenitybdd.rest.SerenityRest;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
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
@PactTestFor(pactVersion = PactSpecVersion.V3)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension.class)
public class IdamConsumerTest {

    private static final String IDAM_DETAILS_URL = "/o/userinfo";

    @Pact(provider = "Idam_api", consumer = "em_npa_api")
    public RequestResponsePact executeGetUserInfoAndGet200(PactDslWithProvider builder) {

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
                .uponReceiving("Provider returns user info to EM Native Pdf Annotator API")
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
    @PactTestFor(pactMethod = "executeGetUserInfoAndGet200")
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
    }

    private DslPart createUserInfoResponse() {
        return new PactDslJsonBody()
                .stringType("uid", "1234-2345-3456-4567")
                .closeArray();

    }

}
