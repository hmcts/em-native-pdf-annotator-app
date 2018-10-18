package uk.gov.hmcts.reform.em.npa.functional.bdd.steps;

import com.google.common.collect.ImmutableMap;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.em.npa.functional.bdd.utils.JsonUtils;
import uk.gov.hmcts.reform.em.npa.functional.bdd.utils.TestContext;
import uk.gov.hmcts.reform.em.npa.functional.bdd.utils.TestTrustManager;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.Optional;

public class BaseSteps {

    protected RestTemplate restTemplate;

    protected static final DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

    @Value("${base-urls.test-url}")
    protected String baseUrl;

    @Value("${base-urls.idam-url}")
    protected String idamUrl;

    @Value("${base-urls.idam-user}")
    protected String idamUser;

    @Value("${base-urls.idam-user-role}")
    protected String idamUserRole;

    @Value("${base-urls.idam-s2s}")
    protected String s2sUrl;

    @Value("${base-urls.s2s-name}")
    protected String s2sName;

    @Value("${base-urls.s2s-token}")
    protected String s2sToken;

    protected TestContext testContext;
    
    protected HttpHeaders header;

    @Autowired
    public BaseSteps(TestContext testContext) {
        this.testContext = testContext;
    }

    public void setup() throws Exception {
        restTemplate = new RestTemplate(TestTrustManager.getInstance().getTestRequestFactory());

        prepareAuthenticationTokens();

        header = new HttpHeaders();
        header.setContentType(MediaType.APPLICATION_JSON);
        Optional.ofNullable(testContext.getHttpContext().getIdamAuthorRef())
            .ifPresent(token -> header.add("Authorization", "Bearer " + token));

        Optional.ofNullable(testContext.getHttpContext().getIdamServiceRef())
            .ifPresent(token -> header.add("ServiceAuthorization", "Bearer " + token));
    }

    private void prepareAuthenticationTokens() throws Exception {
        getIdamToken();
        getS2sToken();
    }

    private void getIdamToken() {

        createUser(idamUser, "password");
        String userId = findUserIdByUserEmail(idamUser).toString();

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.set("id", userId);
        body.set("role", idamUserRole);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> idamResponse = new RestTemplate()
            .postForEntity(idamUrl + "/testing-support/lease", request, String.class);

        if (idamResponse.hasBody()) {
            testContext.getHttpContext().setIdamAuthorRef(idamResponse.getBody());
        }
    }

    private Integer findUserIdByUserEmail(String email) {
        ResponseEntity<Map> responseEntity = new RestTemplate()
                .getForEntity(idamUrl + "/users?email=" + email, Map.class);
        return (Integer) responseEntity.getBody().get("id");
    }

    private void createUser(String email, String password) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(String.format("{ \"email\":\"%s\", \"forename\":\"test\",\"surname\":\"test\",\"password\":\"%s\"}", email, password), headers);
            new RestTemplate().postForEntity(idamUrl + "/testing-support/accounts", request, String.class);
        } catch (HttpClientErrorException e) {
            e.printStackTrace();
        }
    }

    private void getS2sToken() throws Exception {
        String otp = String.valueOf(new GoogleAuthenticator().getTotpPassword(s2sToken));
        ImmutableMap<String, String> params = ImmutableMap.of(
            "microservice", s2sName,
            "oneTimePassword", otp
        );
        String json = JsonUtils.toJson(params);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> httpEntity = new HttpEntity<>(json, headers);

        ResponseEntity<String> s2sResponse = new RestTemplate()
            .postForEntity(s2sUrl + "/lease", httpEntity, String.class);

        if (s2sResponse.hasBody()) {
            testContext.getHttpContext().setIdamServiceRef(s2sResponse.getBody());
        }
    }

//    protected ResponseEntity sendRequest(CohEntityTypes entity, String methodType, String payload) {
//        return sendRequest(entity.toString(), methodType, payload);
//    }
//
//    protected ResponseEntity sendRequest(String entity, String methodType, String payload) {
//        HttpMethod method = HttpMethod.valueOf(methodType);
//
//        CohEndpointHandler endpoint = CohEndpointFactory.getRequestEndpoint(entity);
//        HttpEntity<String> request = new HttpEntity<>(payload, header);
//
//        return restTemplate.exchange(endpoint.getUrl(method, testContext), method, request, String.class);
//    }

    public RestTemplate getRestTemplate() {
        return restTemplate;
    }
}
