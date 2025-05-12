package uk.gov.hmcts.reform.em.npa.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings("squid:S5778")
class RedactionServiceImplTest {

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

    private final List<RedactionDTO> redactions = new ArrayList<>();

    private static final UUID docStoreUUID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
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
    void redactPdfFileTest() throws DocumentTaskProcessingException, IOException {
        File mockFile = new File("prosecution1.pdf");
        Mockito.when(dmStoreDownloader.downloadFile(docStoreUUID.toString())).thenReturn(mockFile);
        Mockito.when(pdfRedaction.redactPdf(mockFile, redactions)).thenReturn(mockFile);

        File result = redactionService.redactFile("jwt", "s2sToken", createRedactionRequest("caseId", docStoreUUID,
            redactions));
        assertEquals(result.getName(), mockFile.getName());
        Mockito.verify(cdamService, Mockito.atLeast(0)).downloadFile("jwt", "s2sToken", docStoreUUID);
    }

    @Test
    void redactPdfFileCdamTest() throws DocumentTaskProcessingException, IOException {
        File mockFile = new File("prosecution1.pdf");
        Mockito.when(cdamService.downloadFile("jwt", "s2sToken", docStoreUUID)).thenReturn(mockFile);
        Mockito.when(pdfRedaction.redactPdf(mockFile, redactions)).thenReturn(mockFile);
        redactionService.cdamEnabled = true;
        RedactionRequest redactionRequest = createRedactionRequest("caseId", docStoreUUID, redactions);

        File result = redactionService.redactFile("jwt", "s2sToken", redactionRequest);
        assertEquals(result.getName(), mockFile.getName());
        Mockito.verify(cdamService, Mockito.atLeast(1)).downloadFile("jwt", "s2sToken", docStoreUUID);
    }

    @Test
    void redactImageFileTest() throws DocumentTaskProcessingException, IOException {
        File mockFile = new File("prosecution2.png");
        Mockito.when(dmStoreDownloader.downloadFile(docStoreUUID.toString())).thenReturn(mockFile);
        Mockito.when(imageRedaction.redactImage(mockFile, redactions.get(0).getRectangles())).thenReturn(mockFile);

        File result = redactionService.redactFile("jwt", "s2sToken",createRedactionRequest("caseId", docStoreUUID,
            redactions));
        assertEquals(result.getName(), mockFile.getName());
        Mockito.verify(cdamService, Mockito.atLeast(0)).downloadFile("jwt", "s2sToken", docStoreUUID);
    }

    @Test
    void redactInvalidFileTest() throws DocumentTaskProcessingException, IOException {

        File mockFile = new File("test.txt");
        Mockito.when(dmStoreDownloader.downloadFile(docStoreUUID.toString())).thenReturn(mockFile);

        assertThrows(FileTypeException.class, () ->
            redactionService.redactFile("jwt", "s2sToken",
                createRedactionRequest("caseId", docStoreUUID, redactions)));
        Mockito.verify(cdamService, Mockito.atLeast(0)).downloadFile("jwt", "s2sToken", docStoreUUID);
    }

    @Test
    void redactDocumentTaskProcessingErrorTest() throws DocumentTaskProcessingException, IOException {

        Mockito.when(dmStoreDownloader.downloadFile(docStoreUUID.toString()))
            .thenThrow(DocumentTaskProcessingException.class);
        assertThrows(RedactionProcessingException.class, () ->
            redactionService.redactFile("jwt", "s2sToken",
                createRedactionRequest("caseId", docStoreUUID, redactions)));
        Mockito.verify(cdamService, Mockito.atLeast(0)).downloadFile("jwt", "s2sToken", docStoreUUID);
    }

    @Test
    void redactIOExceptionTest() throws DocumentTaskProcessingException, IOException {

        File mockFile = new File("prosecution2.png");
        Mockito.when(dmStoreDownloader.downloadFile(docStoreUUID.toString())).thenReturn(mockFile);
        Mockito.when(pdfRedaction.redactPdf(mockFile, redactions)).thenReturn(mockFile);
        Mockito.when(imageRedaction.redactImage(mockFile, redactions.get(0).getRectangles()))
            .thenThrow(IOException.class);
        assertThrows(FileTypeException.class, () ->
            redactionService.redactFile("jwt", "s2sToken",
                createRedactionRequest("caseId", docStoreUUID, redactions)));
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
