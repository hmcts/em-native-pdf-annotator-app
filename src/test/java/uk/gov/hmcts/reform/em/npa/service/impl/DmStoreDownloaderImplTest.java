package uk.gov.hmcts.reform.em.npa.service.impl;

import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.document.am.feign.CaseDocumentClientApi;
import uk.gov.hmcts.reform.em.npa.service.exception.DocumentTaskProcessingException;

import java.io.File;
import java.util.UUID;

@RunWith(MockitoJUnitRunner.class)
public class DmStoreDownloaderImplTest {

    @InjectMocks
    DmStoreDownloaderImpl dmStoreDownloader;

    @Mock
    private CaseDocumentClientApi caseDocumentClientApi;

    @Mock
    private OkHttpClient okHttpClient;

    @Mock
    private AuthTokenGenerator authTokenGenerator;

    private static final UUID docStoreUUID = UUID.randomUUID();

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test(expected = DocumentTaskProcessingException.class)
    public void downloadFile() throws Exception {
        dmStoreDownloader.downloadFile("xxx");
    }

    @Test
    public void downloadFileCdam() throws Exception {
        File mockFile = new File("one-page.pdf");
        FileSystemResource fileSystemResource = new FileSystemResource(mockFile);
        Mockito.when(caseDocumentClientApi.getDocumentBinary("xxx", "serviceAuth", docStoreUUID))
            .thenReturn(ResponseEntity.accepted().body(fileSystemResource));
        dmStoreDownloader.downloadFile("xxx", "serviceAuth", docStoreUUID);
        Mockito.verify(caseDocumentClientApi, Mockito.atLeast(1)).getDocumentBinary("xxx", "serviceAuth", docStoreUUID);
    }
}
