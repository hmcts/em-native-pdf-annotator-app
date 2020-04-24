package uk.gov.hmcts.reform.em.npa.service.impl;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.em.npa.domain.RedactionDTO;
import uk.gov.hmcts.reform.em.npa.redaction.ImageRedaction;
import uk.gov.hmcts.reform.em.npa.redaction.PdfRedaction;
import uk.gov.hmcts.reform.em.npa.service.DmStoreDownloader;
import uk.gov.hmcts.reform.em.npa.service.DmStoreUploader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;


public class RedactionServiceImplTest {

    @InjectMocks
    private RedactionServiceImpl redactionService;

    @Mock
    private DmStoreDownloader dmStoreDownloader;

    @Mock
    private DmStoreUploader dmStoreUploader;

    @Mock
    private PdfRedaction pdfRedaction;

    @Mock
    private ImageRedaction imageRedaction;

    private List<RedactionDTO> redactionDTOList = new ArrayList<>();

    @Before
    public void setUp(){

        MockitoAnnotations.initMocks(this);
        ((RedactionServiceImpl) redactionService).imageExtensionsList = Arrays.asList("png","jpeg");
        initRedactionDTOList();

    }

    public void initRedactionDTOList() {
        for (int i = 0; i < 5 ; i++) {
            RedactionDTO redactionDTO = new RedactionDTO();

            redactionDTO.setPageNumber(i + 1);
            redactionDTO.setXCoordinate(100 * (i + 1));
            redactionDTO.setYCoordinate(100 * (i + 1));
            redactionDTO.setHeight(100 * (i + 1));
            redactionDTO.setWidth(100 * (i + 1));

            redactionDTOList.add(redactionDTO);
        }
    }

    @Test
    public void redactPdfFileTest() throws DocumentTaskProcessingException, IOException {

        UUID docStoreUUID = UUID.randomUUID();
        File mockFile = new File("test.pdf");
        Mockito.when(dmStoreDownloader.downloadFile(docStoreUUID.toString())).thenReturn(mockFile);
        Mockito.when(pdfRedaction.redaction(mockFile, redactionDTOList)).thenReturn(mockFile);

        String result = redactionService.redactFile(docStoreUUID, redactionDTOList);
        Assert.assertEquals(result, docStoreUUID.toString());
    }

    @Test
    public void redactImageFileTest() throws DocumentTaskProcessingException, IOException {

        UUID docStoreUUID = UUID.randomUUID();
        File mockFile = new File("test.png");
        Mockito.when(dmStoreDownloader.downloadFile(docStoreUUID.toString())).thenReturn(mockFile);
        Mockito.when(imageRedaction.redaction(mockFile, redactionDTOList)).thenReturn(mockFile);

        String result = redactionService.redactFile(docStoreUUID, redactionDTOList);
        Assert.assertEquals(result, docStoreUUID.toString());
    }

    @Test(expected = FileTypeException.class)
    public void redactInvalidFileTest() throws DocumentTaskProcessingException {

        UUID docStoreUUID = UUID.randomUUID();
        File mockFile = new File("test.txt");
        Mockito.when(dmStoreDownloader.downloadFile(docStoreUUID.toString())).thenReturn(mockFile);

        redactionService.redactFile(docStoreUUID, redactionDTOList);
    }

    @Test
    public void redactDocumentTaskProcessingErrorTest() throws DocumentTaskProcessingException {

        UUID docStoreUUID = UUID.randomUUID();
        Mockito.when(dmStoreDownloader.downloadFile(docStoreUUID.toString())).thenThrow(DocumentTaskProcessingException.class);

        String result = redactionService.redactFile(docStoreUUID, redactionDTOList);
        Assert.assertEquals(result, docStoreUUID.toString());
    }
}
