package uk.gov.hmcts.reform.em.npa.testutil;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.json.JSONObject;
import org.springframework.http.MediaType;

import java.util.UUID;

public class TestUtil {

    private String s2sToken;
    private String idamToken;

    public void createAnnotationSetForDocumentId(String documentId) throws Exception {
        UUID annotationSetId = UUID.randomUUID();
        JSONObject createAnnotationSet = new JSONObject();
        createAnnotationSet.put("documentId", documentId);
        createAnnotationSet.put("id", annotationSetId);

        authRequest()
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(createAnnotationSet)
                .request("POST", Env.getAnnotationApiUrl() + "/api/annotation-sets");
    }

    public String uploadDocument() throws Exception {

        String newDocUrl = s2sAuthRequest()
                .header("Content-Type", MediaType.MULTIPART_FORM_DATA_VALUE)
                .multiPart("files", "test.pdf",  ClassLoader.getSystemResourceAsStream("annotationTemplate.pdf"), "application/pdf")
                .multiPart("classification", "PUBLIC")
                .request("POST", Env.getDmApiUrl() + "/documents")
                .getBody()
                .jsonPath()
                .get("_embedded.documents[0]._links.self.href");

        return newDocUrl.substring(newDocUrl.lastIndexOf("/") + 1);

    }

    public RequestSpecification authRequest() throws Exception {
        return s2sAuthRequest()
            .header("Authorization", "Bearer " + getIdamToken("test@test.com"));
    }

    public RequestSpecification s2sAuthRequest() throws Exception {
        RestAssured.useRelaxedHTTPSValidation();
        return RestAssured
                .given()
                .header("ServiceAuthorization", "Bearer " + getS2sToken())
                .header("Authorization", "Bearer " + getIdamToken("test@test.com"));
    }

    public String getIdamToken(String username) {
        if (idamToken == null) {
            createUser(username, "password");
            String userId = findUserIdByUserEmail(username).toString();

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", userId);
            jsonObject.put("role", "caseworker");

            Response response = RestAssured
                    .given()
                    .header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                    .formParam("id", userId)
                    .formParam("role", "caseworker")
                    .post(Env.getIdamURL() + "/testing-support/lease");

            idamToken = response.getBody().print();
        }
        return idamToken;
    }

    private Integer findUserIdByUserEmail(String email) {
        return RestAssured
                .get(Env.getIdamURL() + "/users?email=" + email)
                .getBody()
                .jsonPath()
                .get("id");
    }

    public void createUser(String email, String password) {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("email", email);
        jsonObject.put("password", password);
        jsonObject.put("forename", "test");
        jsonObject.put("surname", "test");

        RestAssured
            .given()
            .header("Content-Type", "application/json")
            .body(jsonObject.toString())
            .post(Env.getIdamURL() + "/testing-support/accounts");

    }


    public String getS2sToken() throws Exception {

        if (s2sToken == null) {
            String otp = String.valueOf(new GoogleAuthenticator().getTotpPassword(Env.getS2SToken()));

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("microservice", Env.getS2SServiceName());
            jsonObject.put("oneTimePassword", otp);

            Response response = RestAssured
                    .given()
                    .header("Content-Type", "application/json")
                    .body(jsonObject.toString())
                    .post(Env.getS2SURL() + "/lease");
            s2sToken = response.getBody().asString();
        }

        return s2sToken;

    }

}
