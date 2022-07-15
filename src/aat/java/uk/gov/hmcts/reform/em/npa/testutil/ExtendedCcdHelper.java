package uk.gov.hmcts.reform.em.npa.testutil;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.em.test.ccddata.CcdDataHelper;

@Service
public class ExtendedCcdHelper {

    @Value("${test.url}")
    private String testUrl;

    @Autowired
    private CcdDataHelper ccdDataHelper;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public final String createCaseTemplate = "{\n"
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
                    + "          \"documentLink\": {\n"
                    + "            \"document_url\": \"%s\",\n"
                    + "            \"document_binary_url\": \"%s/binary\",\n"
                    + "            \"document_filename\": \"%s\",\n"
                    + "            \"document_hash\": \"%s\"\n"
                    + "          }\n"
                    + "        }\n"
                    + "      }";

    private String redactionTestUser = "redactionTestUser2@redactiontest.com";

    public CaseDetails createCase(String documents) throws Exception {
        return ccdDataHelper.createCase(redactionTestUser, "PUBLICLAW", getEnvCcdCaseTypeId(), "createCase",
            objectMapper.readTree(String.format(createCaseTemplate, documents)));
    }

    public String getEnvCcdCaseTypeId() {
        return "CCD_BUNDLE_MVP_TYPE_ASYNC";
    }

    public String getCcdDocumentJson(String documentName, String dmUrl, String fileName, String dochash) {
        return String.format(documentTemplate, documentName, dmUrl, dmUrl, fileName, dochash);
    }
}



