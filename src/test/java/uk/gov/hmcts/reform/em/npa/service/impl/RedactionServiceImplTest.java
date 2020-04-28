package uk.gov.hmcts.reform.em.npa.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.em.npa.ccd.client.CcdDataApiCaseUpdater;
import uk.gov.hmcts.reform.em.npa.ccd.client.CcdDataApiEventCreator;
import uk.gov.hmcts.reform.em.npa.ccd.dto.CcdCallbackDto;
import uk.gov.hmcts.reform.em.npa.config.security.SecurityUtils;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.MarkUpDTO;
import uk.gov.hmcts.reform.em.npa.redaction.ImageRedaction;
import uk.gov.hmcts.reform.em.npa.redaction.PdfRedaction;
import uk.gov.hmcts.reform.em.npa.repository.MarkUpRepository;
import uk.gov.hmcts.reform.em.npa.service.DmStoreDownloader;
import uk.gov.hmcts.reform.em.npa.service.DmStoreUploader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;

public class RedactionServiceImplTest {

    @InjectMocks
    private RedactionServiceImpl redactionService;

    @Mock
    private CcdDataApiEventCreator ccdDataApiEventCreator;

    @Mock
    private CcdDataApiCaseUpdater ccdDataApiCaseUpdater;

    @Mock
    private DmStoreDownloader dmStoreDownloader;

    @Mock
    private DmStoreUploader dmStoreUploader;

    @Mock
    private PdfRedaction pdfRedaction;

    @Mock
    private ImageRedaction imageRedaction;

    @Mock
    private MarkUpRepository markUpRepository;

    @Mock
    private SecurityUtils securityUtils;

    private List<MarkUpDTO> markUpDTOList = new ArrayList<>();

    private CcdCallbackDto ccdCallbackDto;

    private ObjectMapper mapper = new ObjectMapper();

    private String caseDataJson = "{" +
            "  \"caseDocuments\":[{" +
            "        \"value\": {" +
            "          \"documentName\": \"Prosecution doc 1\"," +
            "          \"documentType\": \"Prosecution\"," +
            "          \"documentLink\": {" +
            "            \"document_url\":\"documentUrl\"," +
            "            \"document_filename\":\"prosecution1.pdf\"," +
            "            \"document_binary_url\":\"documentUrl/binary\"" +
            "          }," +
            "          \"customDatetimeField\":\"2019-02-07T11:05:20.000\"," +
            "          \"createdBy\":\"Jeroen\"" +
            "        }}, {" +
            "        \"value\": {" +
            "          \"documentName\": \"Prosecution doc 2\"," +
            "          \"documentType\": \"Prosecution\"," +
            "          \"documentLink\": {" +
            "            \"document_url\":\"documentUrl\"," +
            "            \"document_filename\":\"prosecution2.png\"," +
            "            \"document_binary_url\":\"documentUrl/binary\"" +
            "          }," +
            "          \"customDatetimeField\":\"2019-02-07T12:05:20.000\"," +
            "          \"createdBy\":\"Jeroen\"" +
            "        }}" +
            "    ]" +
            "}";

    private String documentStoreResponse = "{" +
            "\"_embedded\": {" +
            "\"documents\": [{" +
            "\"modifiedOn\": \"2020-04-23T14:37:02+0000\"," +
            "\"size\": 19496," +
            "\"createdBy\": \"7f0fd7bf-48c0-4462-9056-38c1190e391f\"," +
            "\"_links\": {" +
            "\"thumbnail\": {" +
            "\"href\": \"http://localhost:4603/documents/0e38e3ad-171f-4d27-bf54-e41f2ed744eb/thumbnail\"" +
            "}," +
            "\"binary\": {" +
            "\"href\": \"http://localhost:4603/documents/0e38e3ad-171f-4d27-bf54-e41f2ed744eb/binary\"" +
            "}," +
            "\"self\": {" +
            "\"href\": \"http://localhost:4603/documents/0e38e3ad-171f-4d27-bf54-e41f2ed744eb\"" +
            "}" +
            "}," +
            "\"lastModifiedBy\": \"7f0fd7bf-48c0-4462-9056-38c1190e391f\"," +
            "\"originalDocumentName\": \"stitched9163237694642183694.pdf\"," +
            "\"mimeType\": \"application/pdf\"," +
            "\"classification\": \"PUBLIC\"," +
            "\"createdOn\": \"2020-04-23T14:37:02+0000\"" +
            "}]" +
            "}" +
            "}";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        redactionService.imageExtensionsList = Arrays.asList("png","jpeg");
        redactionService.ccdEventTrigger = "redactionDocumentUpload";
        initRedactionDTOList();
        ccdCallbackDto = Mockito.mock(CcdCallbackDto.class);
    }

    public void initRedactionDTOList() {
        for (int i = 0; i < 5 ; i++) {
            MarkUpDTO markUpDTO = new MarkUpDTO();

            markUpDTO.setPageNumber(i + 1);
            markUpDTO.setXcoordinate(100 * (i + 1));
            markUpDTO.setYcoordinate(100 * (i + 1));
            markUpDTO.setHeight(100 * (i + 1));
            markUpDTO.setWidth(100 * (i + 1));

            markUpDTOList.add(markUpDTO);
        }
    }

    @Test
    public void redactPdfFileTest() throws DocumentTaskProcessingException, IOException {
        UUID docStoreUUID = UUID.randomUUID();
        File mockFile = new File("prosecution1.pdf");
        Mockito.when(ccdDataApiEventCreator.executeTrigger(eq("caseId"), eq("redactionDocumentUpload"), eq("jwt")))
                .thenReturn(ccdCallbackDto);
        Mockito.when(ccdCallbackDto.getCaseData()).thenReturn(mapper.readTree(caseDataJson));
        Mockito.when(dmStoreDownloader.downloadFile(docStoreUUID.toString())).thenReturn(mockFile);
        Mockito.when(pdfRedaction.redaction(mockFile, markUpDTOList)).thenReturn(mockFile);
        Mockito.when(dmStoreUploader.uploadDocument(mockFile)).thenReturn(mapper.readTree(documentStoreResponse));

        String result = redactionService.redactFile("jwt", "caseId", docStoreUUID, markUpDTOList);
        Assert.assertEquals(result, docStoreUUID.toString());
    }

    @Test
    public void redactImageFileTest() throws DocumentTaskProcessingException, IOException {

        UUID docStoreUUID = UUID.randomUUID();
        File mockFile = new File("prosecution2.png");
        Mockito.when(ccdDataApiEventCreator.executeTrigger(eq("caseId"), eq("redactionDocumentUpload"), eq("jwt")))
                .thenReturn(ccdCallbackDto);
        Mockito.when(ccdCallbackDto.getCaseData()).thenReturn(mapper.readTree(caseDataJson));
        Mockito.when(dmStoreDownloader.downloadFile(docStoreUUID.toString())).thenReturn(mockFile);
        Mockito.when(imageRedaction.redaction(mockFile, markUpDTOList)).thenReturn(mockFile);
        Mockito.when(dmStoreUploader.uploadDocument(mockFile)).thenReturn(mapper.readTree(documentStoreResponse));

        String result = redactionService.redactFile("jwt", "caseId", docStoreUUID, markUpDTOList);
        Assert.assertEquals(result, docStoreUUID.toString());
    }

    @Test(expected = FileTypeException.class)
    public void redactInvalidFileTest() throws DocumentTaskProcessingException {

        UUID docStoreUUID = UUID.randomUUID();
        File mockFile = new File("test.txt");
        Mockito.when(ccdDataApiEventCreator.executeTrigger(eq("caseId"), eq("redactionDocumentUpload"), eq("jwt")))
                .thenReturn(ccdCallbackDto);
        Mockito.when(dmStoreDownloader.downloadFile(docStoreUUID.toString())).thenReturn(mockFile);

        redactionService.redactFile("jwt", "caseId", docStoreUUID, markUpDTOList);
    }

    @Test
    public void redactDocumentTaskProcessingErrorTest() throws DocumentTaskProcessingException {

        UUID docStoreUUID = UUID.randomUUID();
        Mockito.when(ccdDataApiEventCreator.executeTrigger(eq("caseId"), eq("redactionDocumentUpload"), eq("jwt")))
                .thenReturn(ccdCallbackDto);
        Mockito.when(dmStoreDownloader.downloadFile(docStoreUUID.toString())).thenThrow(DocumentTaskProcessingException.class);

        String result = redactionService.redactFile("jwt", "caseId", docStoreUUID, markUpDTOList);
        Assert.assertEquals(result, docStoreUUID.toString());
    }
}
