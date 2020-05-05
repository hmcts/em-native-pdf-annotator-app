package uk.gov.hmcts.reform.em.npa.testutil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.MarkUpDTO;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RectangleDTO;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RedactionDTO;
import uk.gov.hmcts.reform.em.test.ccddefinition.CcdDefinitionHelper;
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

    @Value("${test.url}")
    private String testUrl;

    @Autowired
    private CcdDefinitionHelper ccdDefinitionHelper;

    @Autowired
    private IdamHelper idamHelper;

    @Autowired
    private S2sHelper s2sHelper;

    @Autowired
    private DmHelper dmHelper;

    @Autowired
    private CoreCaseDataApi coreCaseDataApi;

    @Value("${annotation.api.url}")
    private String emAnnotationUrl;
    @Value("${document_management.url}")
    private String dmApiUrl;
    @Value("${document_management.docker_url}")
    private String dmDocumentApiUrl;
    @Value("${ccd.data.api.url}")
    private String ccdDataBaseUrl;

    private String newDocId;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public final String createAutomatedBundlingCaseTemplate = "{\n"
            + "    \"caseTitle\": null,\n"
            + "    \"caseOwner\": null,\n"
            + "    \"caseCreationDate\": null,\n"
            + "    \"caseDescription\": null,\n"
            + "    \"caseComments\": null,\n"
            + "    \"caseDocuments\": [%s]\n"
            + "  }";
    public final String documentTemplate = "{\n"
            + "        \"value\": {\n"
            + "          \"documentName\": \"%s\",\n"
            + "          \"documentType\": \"Prosecution\","
            + "          \"documentLink\": {\n"
            + "            \"document_url\": \"%s\",\n"
            + "            \"document_binary_url\": \"%s/binary\",\n"
            + "            \"document_filename\": \"%s\"\n"
            + "          }\n"
            + "        }\n"
            + "      }";
    private String bundleTesterUser;
    private List<String> bundleTesterUserRoles = Stream.of("caseworker", "caseworker-publiclaw", "ccd-import").collect(Collectors.toList());

    @PostConstruct
    public void init() throws Exception {
        initBundleTesterUser();
        RestAssured.useRelaxedHTTPSValidation();
        idamAuth = idamHelper.authenticateUser(bundleTesterUser);
        s2sAuth = s2sHelper.getS2sToken();

        importCcdDefinitionFile();
    }

    public RedactionDTO populateRedactionDTO(UUID id) {
        RedactionDTO redactionDTO = new RedactionDTO();
        redactionDTO.setRedactionId(id);
        redactionDTO.setPage(1);
        redactionDTO.setDocumentId(id);

        RectangleDTO rectangle = new RectangleDTO();
        rectangle.setId(id);
        rectangle.setX(10.00);
        rectangle.setY(10.00);
        rectangle.setHeight(20.00);
        rectangle.setWidth(30.00);

        redactionDTO.setRectangles(new HashSet<>(Collections.singletonList(rectangle)));
        return redactionDTO;
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

    public void importCcdDefinitionFile() throws Exception {

        ccdDefinitionHelper.importDefinitionFile(
                bundleTesterUser,
                "caseworker-publiclaw",
                getEnvSpecificDefinitionFile());

    }

    public CaseDetails createCase(String documents) throws Exception {
        return createCase(bundleTesterUser, "PUBLICLAW", getEnvCcdCaseTypeId(), "createCase",
                objectMapper.readTree(String.format(createAutomatedBundlingCaseTemplate, documents)));
    }

    public CaseDetails createCase(String username, String jurisdiction, String caseType, String eventId, Object data) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();

        final String userAuthorization = idamHelper.authenticateUser(username);
        String startEventResponseString =
            RestAssured
                .given()
                .header("Authorization", userAuthorization)
                .header("ServiceAuthorization", s2sAuth)
                .header("experimental", "true")
                .request("GET", ccdDataBaseUrl + String.format("/case-types/%s/event-triggers/%s", caseType, eventId))
                .then()
                .extract()
                .body()
                .asString();

        StartEventResponse startEventResponse = mapper.readValue(startEventResponseString, StartEventResponse.class);

        return RestAssured
                .given()
                .header("Authorization", userAuthorization)
                .header("ServiceAuthorization", s2sAuth)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(CaseDataContent.builder()
                        .event(Event.builder().id(startEventResponse.getEventId()).build())
                        .eventToken(startEventResponse.getToken())
                        .data(data).build())
                .request("POST",ccdDataBaseUrl +
                        String.format("/caseworkers/%s/jurisdictions/%s/case-types/%s/cases",
                                idamHelper.getUserId(username),
                                jurisdiction,
                                caseType))
                .then().log().all()
                .extract()
                .body()
                .as(CaseDetails.class);
    }

    public String getEnvCcdCaseTypeId() {
        return String.format("BUND_ASYNC_%d", testUrl.hashCode());
    }

    public InputStream getEnvSpecificDefinitionFile() throws Exception {
        Workbook workbook = new XSSFWorkbook(ClassLoader.getSystemResourceAsStream("adv_bundling_functional_tests_ccd_def.xlsx"));
        Sheet caseTypeSheet = workbook.getSheet("CaseType");

        caseTypeSheet.getRow(3).getCell(3).setCellValue(getEnvCcdCaseTypeId());

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            for (Row row : sheet) {
                for (Cell cell : row) {
                    if (cell.getCellType().equals(CellType.STRING)
                            && cell.getStringCellValue().trim().equals("CCD_BUNDLE_MVP_TYPE_ASYNC")) {
                        cell.setCellValue(getEnvCcdCaseTypeId());
                    }
                    if (cell.getCellType().equals(CellType.STRING)
                            && cell.getStringCellValue().trim().equals("bundle-tester@gmail.com")) {
                        cell.setCellValue(bundleTesterUser);
                    }
                }
            }
        }

        File outputFile = File.createTempFile("ccd", "ftest-def");

        try (FileOutputStream fileOutputStream = new FileOutputStream(outputFile)) {
            workbook.write(fileOutputStream);
        }

        return new FileInputStream(outputFile);
    }

    public void initBundleTesterUser() {
        bundleTesterUser = String.format("bundle-tester-%d@gmail.com", testUrl.hashCode());
        idamHelper.createUser(bundleTesterUser, bundleTesterUserRoles);
    }

    public String getCcdDocumentJson(String documentName, String dmUrl, String fileName) {
        return String.format(documentTemplate, documentName, dmUrl, dmUrl, fileName);
    }
}
