package uk.gov.hmcts.reform.em.npa.testutil;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RectangleDTO;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RedactionDTO;
import uk.gov.hmcts.reform.em.test.dm.DmHelper;
import uk.gov.hmcts.reform.em.test.idam.IdamHelper;
import uk.gov.hmcts.reform.em.test.s2s.S2sHelper;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class TestUtil {

    private String annotationSetId;

    private String idamAuth;
    private String s2sAuth;

    @Autowired
    private IdamHelper idamHelper;

    @Autowired
    private S2sHelper s2sHelper;

    @Autowired
    private DmHelper dmHelper;

    @Value("${annotation.api.url}")
    private String emAnnotationUrl;
    @Value("${document_management.url}")
    private String dmApiUrl;
    @Value("${document_management.docker_url}")
    private String dmDocumentApiUrl;

    @PostConstruct
    public void init() throws Exception {
        idamHelper.createUser("a@b.com", Stream.of("caseworker").collect(Collectors.toList()));
        RestAssured.useRelaxedHTTPSValidation();
        idamAuth = idamHelper.authenticateUser("a@b.com");
        s2sAuth = s2sHelper.getS2sToken();
    }

    public RedactionDTO createRedactionDTO(UUID docId, UUID redactionId) {
        RedactionDTO redactionDTO = new RedactionDTO();
        redactionDTO.setDocumentId(docId);
        redactionDTO.setRedactionId(redactionId);
        redactionDTO.setPage(6);
        Set<RectangleDTO> rectangles = new HashSet<>();
        rectangles.add(createRectangleDTO());
        redactionDTO.setRectangles(rectangles);
        return redactionDTO;
    }

    private RectangleDTO createRectangleDTO() {
        RectangleDTO rectangleDTO = new RectangleDTO();
        rectangleDTO.setId(UUID.randomUUID());
        rectangleDTO.setHeight(10.0);
        rectangleDTO.setWidth(10.0);
        rectangleDTO.setX(20.0);
        rectangleDTO.setY(30.0);
        return rectangleDTO;
    }

    public File getDocumentBinary(String documentId) throws Exception {
        Path tempPath = Paths.get(System.getProperty("java.io.tmpdir") + "/" + documentId + "-test.pdf");
        Files.copy(dmHelper.getDocumentBinary(documentId), tempPath, StandardCopyOption.REPLACE_EXISTING);
        return tempPath.toFile();
    }

    public String saveAnnotation(String annotationSetId, Integer pageNum) {
        UUID annotationId = UUID.randomUUID();
        JSONObject createAnnotations = new JSONObject();
        createAnnotations.put("annotationSetId", annotationSetId);
        createAnnotations.put("id", annotationId);
        createAnnotations.put("annotationType", "highlight");
        createAnnotations.put("page", pageNum);
        createAnnotations.put("color", "d1d1d1");

        JSONArray comments = new JSONArray();
        JSONObject comment = new JSONObject();
        comment.put("content", "text");
        comment.put("annotationId", annotationId);
        comment.put("id", UUID.randomUUID().toString());
        comments.put(0, comment);
        createAnnotations.put("comments", comments);

        JSONArray rectangles = new JSONArray();
        JSONObject rectangle = new JSONObject();
        rectangle.put("id", UUID.randomUUID().toString());
        rectangle.put("annotationId", annotationId);
        rectangle.put("x", 0f);
        rectangle.put("y", 0f);
        rectangle.put("width", 10f);
        rectangle.put("height", 10f);
        rectangles.put(0, rectangle);
        createAnnotations.put("rectangles", rectangles);

        Response response = authRequest()
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(createAnnotations)
                .request("POST", emAnnotationUrl + "/api/annotations");

        Assert.assertEquals(201, response.getStatusCode());

        return annotationId.toString();
    }

    public String saveAnnotation(String annotationSetId) throws Exception {
        return saveAnnotation(annotationSetId, 1);
    }

    public String createAnnotationSetForDocumentId(String documentId) throws Exception {
        UUID annotationSetId = UUID.randomUUID();
        JSONObject createAnnotationSet = new JSONObject();
        createAnnotationSet.put("documentId", documentId);
        createAnnotationSet.put("id", annotationSetId);

        Response response = authRequest()
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(createAnnotationSet)
                .request("POST", emAnnotationUrl + "/api/annotation-sets");

        Assert.assertEquals(201, response.getStatusCode());

        this.annotationSetId = annotationSetId.toString();
        return this.annotationSetId;
    }

    public String uploadDocument(String pdfName) throws Exception {
        return dmHelper.uploadAndGetId(ClassLoader.getSystemResourceAsStream(pdfName), "application/pdf", pdfName);
    }

    public String uploadDocument() throws Exception {
        return uploadDocument("annotationTemplate.pdf");
    }

    public String uploadDocumentAndReturnUrl(String fileName, String mimeType) {
        try {
            String url = dmHelper.getDocumentMetadata(
                    dmHelper.uploadAndGetId(
                            ClassLoader.getSystemResourceAsStream(fileName), mimeType, fileName))
                    .links.self.href;

            return getDmApiUrl().equals("http://localhost:4603")
                    ? url.replaceAll(getDmApiUrl(), getDmDocumentApiUrl())
                    : url;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String uploadPdfDocumentAndReturnUrl() {
        return uploadDocumentAndReturnUrl("annotationTemplate.pdf", "application/pdf");
    }

    public String uploadImageDocumentAndReturnUrl() {
        return uploadDocumentAndReturnUrl("fist.png", "image/png");
    }

    public String uploadRichTextDocumentAndReturnUrl() {
        return uploadDocumentAndReturnUrl("test.rtf", "application/rtf");
    }

    public String getDmApiUrl() {
        return dmApiUrl;
    }

    public String getDmDocumentApiUrl() {
        return dmDocumentApiUrl;
    }

    public RequestSpecification authRequest() {
        return s2sAuthRequest()
                .header("Authorization", idamAuth);
    }

    public RequestSpecification s2sAuthRequest() {
        return RestAssured
                .given()
                .log().all()
                .header("ServiceAuthorization", s2sAuth);
    }

    public RequestSpecification emptyIdamAuthRequest() {
        return s2sAuthRequest()
                .header("Authorization", null);
    }

    public RequestSpecification emptyIdamAuthAndEmptyS2SAuth() {
        return RestAssured
                .given()
                .header("ServiceAuthorization", null)
                .header("Authorization", null);
    }

    public RequestSpecification validAuthRequestWithEmptyS2SAuth() {
        return emptyS2sAuthRequest().header("Authorization", idamAuth);
    }

    public RequestSpecification validS2SAuthWithEmptyIdamAuth() {

        return s2sAuthRequest().header("Authorization", null);
    }

    private RequestSpecification emptyS2sAuthRequest() {

        return RestAssured.given().header("ServiceAuthorization", null);
    }

    public RequestSpecification invalidIdamAuthrequest() {

        return s2sAuthRequest().header("Authorization", "invalidIDAMAuthRequest");
    }

    public RequestSpecification invalidS2SAuth() {

        return invalidS2sAuthRequest().header("Authorization", idamAuth);
    }

    private RequestSpecification invalidS2sAuthRequest() {

        return RestAssured.given().header("ServiceAuthorization", "invalidS2SAuthorization");
    }
}
