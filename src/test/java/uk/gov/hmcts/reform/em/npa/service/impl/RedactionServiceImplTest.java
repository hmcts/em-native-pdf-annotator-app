package uk.gov.hmcts.reform.em.npa.service.impl;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.em.npa.Application;
import uk.gov.hmcts.reform.em.npa.TestSecurityConfiguration;
import uk.gov.hmcts.reform.em.npa.redaction.ImageRedaction;
import uk.gov.hmcts.reform.em.npa.redaction.PdfRedaction;
import uk.gov.hmcts.reform.em.npa.service.DmStoreDownloader;
import uk.gov.hmcts.reform.em.npa.service.DmStoreUploader;
import uk.gov.hmcts.reform.em.npa.service.RedactionService;
import uk.gov.hmcts.reform.em.npa.domain.RedactionDTO;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Application.class, TestSecurityConfiguration.class})
public class RedactionServiceImplTest {

    @Autowired
    private RedactionService redactionService;

    @MockBean
    private DmStoreDownloader dmStoreDownloader;
    @MockBean
    private DmStoreUploader dmStoreUploader;
    @MockBean
    private PdfRedaction pdfRedaction;
    @MockBean
    private ImageRedaction imageRedaction;

    private List<RedactionDTO> redactionDTOList = new ArrayList<>();

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
        initRedactionDTOList();

        String result = redactionService.redactFile(docStoreUUID, redactionDTOList);
        Assert.assertEquals(result, docStoreUUID.toString());
    }

    @Test
    public void redactImageFileTest() throws DocumentTaskProcessingException, IOException {
        UUID docStoreUUID = UUID.randomUUID();
        File mockFile = new File("test.png");

        Mockito.when(dmStoreDownloader.downloadFile(docStoreUUID.toString())).thenReturn(mockFile);
        Mockito.when(imageRedaction.redaction(mockFile, redactionDTOList)).thenReturn(mockFile);
        initRedactionDTOList();

        String result = redactionService.redactFile(docStoreUUID, redactionDTOList);
        Assert.assertEquals(result, docStoreUUID.toString());
    }

    @Test(expected = FileTypeException.class)
    public void redactInvalidFileTest() throws DocumentTaskProcessingException {
        UUID docStoreUUID = UUID.randomUUID();
        File mockFile = new File("test.txt");

        Mockito.when(dmStoreDownloader.downloadFile(docStoreUUID.toString())).thenReturn(mockFile);
        initRedactionDTOList();
        redactionService.redactFile(docStoreUUID, redactionDTOList);
    }

    @Test
    public void redactDocumentTaskProcessingErrorTest() throws DocumentTaskProcessingException {
        UUID docStoreUUID = UUID.randomUUID();

        Mockito.when(dmStoreDownloader.downloadFile(docStoreUUID.toString())).thenThrow(DocumentTaskProcessingException.class);
        initRedactionDTOList();

        String result = redactionService.redactFile(docStoreUUID, redactionDTOList);
        Assert.assertEquals(result, docStoreUUID.toString());
    }
}
