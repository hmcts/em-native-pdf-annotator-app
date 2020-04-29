package uk.gov.hmcts.reform.em.npa.testutil;

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
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.em.test.ccddata.CcdDataHelper;
import uk.gov.hmcts.reform.em.test.ccddefinition.CcdDefinitionHelper;
import uk.gov.hmcts.reform.em.test.dm.DmHelper;
import uk.gov.hmcts.reform.em.test.idam.IdamHelper;
import uk.gov.hmcts.reform.em.test.s2s.S2sHelper;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
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

    @Value("${test.url}")
    private String testUrl;

    @Autowired
    private CcdDataHelper ccdDataHelper;

    @Autowired
    private CcdDefinitionHelper ccdDefinitionHelper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public final String createCaseTemplate = "{\n"
            + "    \"caseTitle\": null,\n"
            + "    \"caseOwner\": null,\n"
            + "    \"caseCreationDate\": null,\n"
            + "    \"caseDescription\": null,\n"
            + "    \"caseComments\": null,\n"
            + "    \"caseDocuments\": [%s],\n"
            + "    \"bundleConfiguration\": \"f-tests-1-flat-docs.yaml\"\n"
            + "  }";
    public final String documentTemplate = "{\n"
            + "        \"value\": {\n"
            + "          \"documentName\": \"%s\",\n"
            + "          \"documentLink\": {\n"
            + "            \"document_url\": \"%s\",\n"
            + "            \"document_binary_url\": \"%s/binary\",\n"
            + "            \"document_filename\": \"%s\"\n"
            + "          }\n"
            + "        }\n"
            + "      }";
    private String testerUser;
    private List<String> testerUserRoles = Stream.of("caseworker", "caseworker-publiclaw", "ccd-import").collect(Collectors.toList());

    @PostConstruct
    public void init() throws Exception {
        idamHelper.createUser("a@b.com", Stream.of("caseworker").collect(Collectors.toList()));
        RestAssured.useRelaxedHTTPSValidation();
        idamAuth = idamHelper.authenticateUser("a@b.com");
        s2sAuth = s2sHelper.getS2sToken();
        initTesterUser();
        importCcdDefinitionFile();
    }


    public void importCcdDefinitionFile() throws Exception {
        ccdDefinitionHelper.importDefinitionFile(
                testerUser,
                "caseworker-publiclaw",
                getEnvSpecificDefinitionFile());
    }

    public CaseDetails createCase(String documents) throws Exception {
        return ccdDataHelper.createCase(testerUser, "PUBLICLAW", getEnvCcdCaseTypeId(), "createCase",
                objectMapper.readTree(String.format(createCaseTemplate, documents)));
    }

    public String getEnvCcdCaseTypeId() {
        return String.format("BUND_ASYNC_%d", testUrl.hashCode());
    }

    public InputStream getEnvSpecificDefinitionFile() throws Exception {
        Workbook workbook = new XSSFWorkbook(ClassLoader.getSystemResourceAsStream("npa_functional_tests_ccd_def.xlsx"));

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
                            && cell.getStringCellValue().trim().equals("tester@gmail.com")) {
                        cell.setCellValue(testerUser);
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

    public void initTesterUser() {
        testerUser = String.format("tester-%d@gmail.com", testUrl.hashCode());
        idamHelper.createUser(testerUser, testerUserRoles);
    }

    public String getCcdDocumentJson(String documentName, String dmUrl, String fileName) {
        return String.format(documentTemplate, documentName, dmUrl, dmUrl, fileName);
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
