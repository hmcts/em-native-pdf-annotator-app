package uk.gov.hmcts.reform.em.npa.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.ccd.document.am.feign.CaseDocumentClientApi;
import uk.gov.hmcts.reform.ccd.document.am.model.Document;
import uk.gov.hmcts.reform.em.npa.service.exception.DocumentTaskProcessingException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CdamServiceTest {

    private CdamService cdamService;

    @Mock
    private CaseDocumentClientApi caseDocumentClientApi;

    @Mock
    private ByteArrayResource byteArrayResource;

    private static final UUID docStoreUUID = UUID.randomUUID();

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        cdamService = new CdamService(caseDocumentClientApi);
    }

    @Test
    void downloadFileCdam() throws Exception {

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

    @Test
    void downloadFileCdamNullResponseBody() {

        ResponseEntity responseEntity = ResponseEntity.accepted().body(null);
        when(caseDocumentClientApi.getDocumentBinary("xxx", "serviceAuth", docStoreUUID)).thenReturn(responseEntity);

        assertThrows(DocumentTaskProcessingException.class, () ->
            cdamService.downloadFile("xxx", "serviceAuth", docStoreUUID));

    }

    @Test
    void downloadFileCdamNullResponse() {
        when(caseDocumentClientApi.getDocumentBinary("xxx", "serviceAuth", docStoreUUID)).thenReturn(null);

        DocumentTaskProcessingException exception = assertThrows(DocumentTaskProcessingException.class, () ->
            cdamService.downloadFile("xxx", "serviceAuth", docStoreUUID));

        assertEquals("Could not access the binary. HTTP response: null", exception.getMessage());
    }

    @Test
    void downloadFileCdamWithoutFileExtension() throws Exception {
        Document document = Document.builder().originalDocumentName("").build();
        when(caseDocumentClientApi.getMetadataForDocument("xxx", "serviceAuth", docStoreUUID))
            .thenReturn(document);
        when(byteArrayResource.getInputStream()).thenReturn(InputStream.nullInputStream());
        when(caseDocumentClientApi.getDocumentBinary("xxx", "serviceAuth", docStoreUUID))
            .thenReturn(ResponseEntity.accepted().body(byteArrayResource));

        File downloadedFile = cdamService.downloadFile("xxx", "serviceAuth", docStoreUUID);

        assertTrue(downloadedFile.exists());
        assertTrue(downloadedFile.getName().startsWith("document-"));
        assertTrue(downloadedFile.getName().endsWith(".tmp"));
    }

    @Test
    void downloadFileCdamInputStreamFailure() throws Exception {
        Document document = Document.builder().originalDocumentName("one-page.pdf").build();
        InputStream failingInputStream = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("read failed");
            }
        };
        when(caseDocumentClientApi.getMetadataForDocument("xxx", "serviceAuth", docStoreUUID))
            .thenReturn(document);
        when(byteArrayResource.getInputStream()).thenReturn(failingInputStream);
        when(caseDocumentClientApi.getDocumentBinary("xxx", "serviceAuth", docStoreUUID))
            .thenReturn(ResponseEntity.accepted().body(byteArrayResource));

        DocumentTaskProcessingException exception = assertThrows(DocumentTaskProcessingException.class, () ->
            cdamService.downloadFile("xxx", "serviceAuth", docStoreUUID));

        assertEquals("Could not copy the file to a temp location", exception.getMessage());
        assertEquals("read failed", exception.getCause().getMessage());
    }

    @Test
    void downloadFileCdamFailsWhenTempRootIsNotDirectory() throws Exception {
        Path temporaryRoot = Files.createTempDirectory("em-npa-test-");
        Files.createFile(temporaryRoot.resolve("em-npa"));
        String originalTempDirectory = System.getProperty("java.io.tmpdir");
        System.setProperty("java.io.tmpdir", temporaryRoot.toString());

        try {
            Document document = Document.builder().originalDocumentName("one-page.pdf").build();
            when(caseDocumentClientApi.getMetadataForDocument("xxx", "serviceAuth", docStoreUUID))
                .thenReturn(document);
            when(byteArrayResource.getInputStream()).thenReturn(InputStream.nullInputStream());
            when(caseDocumentClientApi.getDocumentBinary("xxx", "serviceAuth", docStoreUUID))
                .thenReturn(ResponseEntity.accepted().body(byteArrayResource));

            DocumentTaskProcessingException exception = assertThrows(DocumentTaskProcessingException.class, () ->
                cdamService.downloadFile("xxx", "serviceAuth", docStoreUUID));

            assertEquals("Could not copy the file to a temp location", exception.getMessage());
            assertEquals("Temporary directory is not a directory", exception.getCause().getMessage());
        } finally {
            System.setProperty("java.io.tmpdir", originalTempDirectory);
            Files.deleteIfExists(temporaryRoot.resolve("em-npa"));
            Files.deleteIfExists(temporaryRoot);
        }
    }

}

