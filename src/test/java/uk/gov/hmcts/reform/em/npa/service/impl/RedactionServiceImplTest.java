package uk.gov.hmcts.reform.em.npa.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.em.npa.config.security.SecurityUtils;
import uk.gov.hmcts.reform.em.npa.redaction.ImageRedaction;
import uk.gov.hmcts.reform.em.npa.redaction.PdfRedaction;
import uk.gov.hmcts.reform.em.npa.repository.MarkUpRepository;
import uk.gov.hmcts.reform.em.npa.service.CdamService;
import uk.gov.hmcts.reform.em.npa.service.DmStoreDownloader;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RectangleDTO;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RedactionDTO;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RedactionRequest;
import uk.gov.hmcts.reform.em.npa.service.exception.DocumentTaskProcessingException;
import uk.gov.hmcts.reform.em.npa.service.exception.FileTypeException;
import uk.gov.hmcts.reform.em.npa.service.exception.RedactionProcessingException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public class RedactionServiceImplTest {

    @InjectMocks
    private RedactionServiceImpl redactionService;

    @Mock
    private DmStoreDownloader dmStoreDownloader;

    @Mock
    private PdfRedaction pdfRedaction;

    @Mock
    private ImageRedaction imageRedaction;

    @Mock
    private MarkUpRepository markUpRepository;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private CdamService cdamService;

    private List<RedactionDTO> redactions = new ArrayList<>();

    private ObjectMapper mapper = new ObjectMapper();

    private String documentStoreResponse = "{_embedded\": {documents\": [{"
        + "\"modifiedOn\": \"2020-04-23T14:37:02+0000\","
        + "\"size\": 19496,"
        + "\"createdBy\": \"7f0fd7bf-48c0-4462-9056-38c1190e391f\","
        + "\"_links\": {"
        + "\"thumbnail\": {"
        + "\"href\": \"http://localhost:4603/documents/0e38e3ad-171f-4d27-bf54-e41f2ed744eb/thumbnail\""
        + "},\"binary\": {"
        + "\"href\": \"http://localhost:4603/documents/0e38e3ad-171f-4d27-bf54-e41f2ed744eb/binary\""
        + "},\"self\": {"
        + "\"href\": \"http://localhost:4603/documents/0e38e3ad-171f-4d27-bf54-e41f2ed744eb\""
        + "}},\"lastModifiedBy\": \"7f0fd7bf-48c0-4462-9056-38c1190e391f\","
        + "\"originalDocumentName\": \"stitched9163237694642183694.pdf\","
        + "\"mimeType\": \"application/pdf\",\"classification\": \"PUBLIC\","
        + "\"createdOn\": \"2020-04-23T14:37:02+0000\"}]}}";

    private static final UUID docStoreUUID = UUID.randomUUID();

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        redactionService.imageExtensionsList = Arrays.asList("png","jpeg");
        initRedactionDTOList();
    }

    public void initRedactionDTOList() {
        for (int i = 0; i < 5; i++) {
            RedactionDTO redaction = new RedactionDTO();
            redaction.setRedactionId(UUID.randomUUID());
            redaction.setDocumentId(UUID.randomUUID());
            redaction.setPage(i);

            RectangleDTO rectangle = new RectangleDTO();
            rectangle.setId(UUID.randomUUID());
            rectangle.setX(100.00);
            rectangle.setY(100.00);
            rectangle.setHeight(100.00);
            rectangle.setWidth(100.00);

            redaction.setRectangles(new HashSet<>(Collections.singletonList(rectangle)));

            redactions.add(redaction);
        }
    }

    @Test
    public void redactPdfFileTest() throws DocumentTaskProcessingException, IOException {
        File mockFile = new File("prosecution1.pdf");
        Mockito.when(dmStoreDownloader.downloadFile(docStoreUUID.toString())).thenReturn(mockFile);
        Mockito.when(pdfRedaction.redactPdf(mockFile, redactions)).thenReturn(mockFile);

        File result = redactionService.redactFile("jwt", "s2sToken", createRedactionRequest("caseId", docStoreUUID,
            redactions));
        Assert.assertEquals(result.getName(), mockFile.getName());
        Mockito.verify(cdamService, Mockito.atLeast(0)).downloadFile("jwt", "s2sToken", docStoreUUID);
    }

    @Test
    public void redactPdfFileCdamTest() throws DocumentTaskProcessingException, IOException {
        File mockFile = new File("prosecution1.pdf");
        Mockito.when(cdamService.downloadFile("jwt", "s2sToken", docStoreUUID)).thenReturn(mockFile);
        Mockito.when(pdfRedaction.redactPdf(mockFile, redactions)).thenReturn(mockFile);
        redactionService.cdamEnabled = true;
        RedactionRequest redactionRequest = createRedactionRequest("caseId", docStoreUUID, redactions);

        File result = redactionService.redactFile("jwt", "s2sToken", redactionRequest);
        Assert.assertEquals(result.getName(), mockFile.getName());
        Mockito.verify(cdamService, Mockito.atLeast(1)).downloadFile("jwt", "s2sToken", docStoreUUID);
    }

    @Test
    public void redactImageFileTest() throws DocumentTaskProcessingException, IOException {
        File mockFile = new File("prosecution2.png");
        Mockito.when(dmStoreDownloader.downloadFile(docStoreUUID.toString())).thenReturn(mockFile);
        Mockito.when(imageRedaction.redactImage(mockFile, redactions.get(0).getRectangles())).thenReturn(mockFile);

        File result = redactionService.redactFile("jwt", "s2sToken",createRedactionRequest("caseId", docStoreUUID,
            redactions));
        Assert.assertEquals(result.getName(), mockFile.getName());
        Mockito.verify(cdamService, Mockito.atLeast(0)).downloadFile("jwt", "s2sToken", docStoreUUID);
    }

    @Test(expected = FileTypeException.class)
    public void redactInvalidFileTest() throws DocumentTaskProcessingException, IOException {

        UUID docStoreUUID = UUID.randomUUID();
        File mockFile = new File("test.txt");
        Mockito.when(dmStoreDownloader.downloadFile(docStoreUUID.toString())).thenReturn(mockFile);

        redactionService.redactFile("jwt", "s2sToken",createRedactionRequest("caseId", docStoreUUID, redactions));
        Mockito.verify(cdamService, Mockito.atLeast(0)).downloadFile("jwt", "s2sToken", docStoreUUID);
    }

    @Test(expected = RedactionProcessingException.class)
    public void redactDocumentTaskProcessingErrorTest() throws DocumentTaskProcessingException, IOException {

        UUID docStoreUUID = UUID.randomUUID();
        Mockito.when(dmStoreDownloader.downloadFile(docStoreUUID.toString()))
            .thenThrow(DocumentTaskProcessingException.class);

        redactionService.redactFile("jwt", "s2sToken",createRedactionRequest("caseId", docStoreUUID, redactions));
        Mockito.verify(cdamService, Mockito.atLeast(0)).downloadFile("jwt", "s2sToken", docStoreUUID);
    }

    private RedactionRequest createRedactionRequest(String caseId, UUID docStoreUUID, List<RedactionDTO> redactions) {
        RedactionRequest redactionRequest = new RedactionRequest();
        redactionRequest.setCaseId(caseId);
        redactionRequest.setDocumentId(docStoreUUID);
        redactionRequest.setRedactions(redactions);
        return redactionRequest;
    }
}
