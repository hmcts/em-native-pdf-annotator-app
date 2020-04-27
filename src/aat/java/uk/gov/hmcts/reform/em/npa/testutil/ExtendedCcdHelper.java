package uk.gov.hmcts.reform.em.npa.testutil;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.em.test.ccddata.CcdDataHelper;
import uk.gov.hmcts.reform.em.test.ccddefinition.CcdDefinitionHelper;
import uk.gov.hmcts.reform.em.test.idam.IdamHelper;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ExtendedCcdHelper {

    @Value("${test.url}")
    private String testUrl;

    @Autowired
    private IdamHelper idamHelper;

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
}
