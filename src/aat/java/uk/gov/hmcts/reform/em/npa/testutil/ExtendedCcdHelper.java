package uk.gov.hmcts.reform.em.npa.testutil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.em.test.ccddata.CcdDataHelper;

@Service
public class ExtendedCcdHelper {

    @Value("${test.url}")
    private String testUrl;

    private final CcdDataHelper ccdDataHelper;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public static final String CREATE_CASE_TEMPLATE = """
        {
            "caseTitle": null,
            "caseOwner": null,
            "caseCreationDate": null,
            "caseDescription": null,
            "caseComments": null,
            "caseDocuments": [%s]
          }""";
    public static final String DOCUMENT_TEMPLATE = """
        {
                "value": {
                  "documentName": "%s",
                  "documentLink": {
                    "document_url": "%s",
                    "document_binary_url": "%s/binary",
                    "document_filename": "%s",
                    "document_hash": "%s"
                  }
                }
              }""";

    public ExtendedCcdHelper(CcdDataHelper ccdDataHelper) {
        this.ccdDataHelper = ccdDataHelper;
    }

    public CaseDetails createCase(String documents) throws JsonProcessingException {
        String redactionTestUser = "redactionTestUser2@redactiontest.com";
        return ccdDataHelper.createCase(redactionTestUser, "PUBLICLAW", getEnvCcdCaseTypeId(), "createCase",
            objectMapper.readTree(String.format(CREATE_CASE_TEMPLATE, documents)));
    }

    public String getEnvCcdCaseTypeId() {
        return "CCD_BUNDLE_MVP_TYPE_ASYNC";
    }

    public String getCcdDocumentJson(String documentName, String dmUrl, String fileName, String dochash) {
        return String.format(DOCUMENT_TEMPLATE, documentName, dmUrl, dmUrl, fileName, dochash);
    }
}



