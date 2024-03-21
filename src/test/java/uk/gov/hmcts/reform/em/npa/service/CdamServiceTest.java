package uk.gov.hmcts.reform.em.npa.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.ccd.document.am.feign.CaseDocumentClientApi;
import uk.gov.hmcts.reform.ccd.document.am.model.Document;
import uk.gov.hmcts.reform.em.npa.service.exception.DocumentTaskProcessingException;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CdamServiceTest {

    private CdamService cdamService;

    @Mock
    private CaseDocumentClientApi caseDocumentClientApi;

    @Mock
    private ByteArrayResource byteArrayResource;

    private static final UUID docStoreUUID = UUID.randomUUID();

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        cdamService = new CdamService(caseDocumentClientApi);
    }

    @Test
    public void downloadFileCdam() throws Exception {

        Document document = Document.builder().originalDocumentName("one-page.pdf").build();
        File mockFile = new File("src/test/resources/one-page.pdf");
        InputStream inputStream = new FileInputStream(mockFile);

        when(caseDocumentClientApi.getMetadataForDocument("xxx", "serviceAuth", docStoreUUID))
            .thenReturn(document);
        ResponseEntity responseEntity = ResponseEntity.accepted().body(byteArrayResource);
        when(byteArrayResource.getInputStream()).thenReturn(inputStream);
        when(caseDocumentClientApi.getDocumentBinary("xxx", "serviceAuth", docStoreUUID)).thenReturn(responseEntity);

        cdamService.downloadFile("xxx", "serviceAuth", docStoreUUID);

        verify(caseDocumentClientApi, Mockito.atLeast(1)).getDocumentBinary("xxx", "serviceAuth", docStoreUUID);
        verify(caseDocumentClientApi, Mockito.atLeast(1)).getMetadataForDocument("xxx", "serviceAuth", docStoreUUID);
    }

    @Test(expected = DocumentTaskProcessingException.class)
    public void downloadFileCdamNullResponseBody() throws Exception {

        ResponseEntity responseEntity = ResponseEntity.accepted().body(null);
        when(caseDocumentClientApi.getDocumentBinary("xxx", "serviceAuth", docStoreUUID)).thenReturn(responseEntity);

        cdamService.downloadFile("xxx", "serviceAuth", docStoreUUID);

    }

}

