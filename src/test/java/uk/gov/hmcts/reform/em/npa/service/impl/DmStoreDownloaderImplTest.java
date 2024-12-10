package uk.gov.hmcts.reform.em.npa.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.mock.MockInterceptor;
import okhttp3.mock.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.em.npa.service.exception.DocumentTaskProcessingException;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

class DmStoreDownloaderImplTest {

    DmStoreDownloaderImpl dmStoreDownloader;

    @Mock
    AuthTokenGenerator authTokenGenerator;

    @Mock
    MockInterceptor interceptor;

    @Mock
    OkHttpClient okHttpClient;

    @Mock
    Call call;

    @BeforeEach
    public void setup() {
        interceptor = new MockInterceptor();

        authTokenGenerator = Mockito.mock(AuthTokenGenerator.class);

        dmStoreDownloader = new DmStoreDownloaderImpl(okHttpClient,
                authTokenGenerator,
                "http://localhost:4603",
                new ObjectMapper());
    }

    @Test
    void invalidDocumentId() {
        assertThrows(DocumentTaskProcessingException.class, () ->
            dmStoreDownloader.downloadFile("abc"));
    }

    @Test
    void testRuntimeExceptionThrown() {
        UUID dmStoreDocId = UUID.randomUUID();
        assertThrows(DocumentTaskProcessingException.class, () ->
            dmStoreDownloader.downloadFile(dmStoreDocId.toString()));
    }

    @Disabled("Unable to stimulate IOException")
    @Test
    void testIOExceptionThrown() throws DocumentTaskProcessingException {

        UUID dmStoreDocId = UUID.randomUUID();
        Mockito.when(dmStoreDownloader.downloadFile(dmStoreDocId.toString())).thenThrow(IOException.class);
        assertThrows(DocumentTaskProcessingException.class, () ->
            dmStoreDownloader.downloadFile(dmStoreDocId.toString()));

    }

    @Test
    void downloadFile() {
        assertThrows(DocumentTaskProcessingException.class, () ->
            dmStoreDownloader.downloadFile("xxx"));
    }

    @Test
    void testDownloadAFile() {
        UUID dmStoreDocId = UUID.randomUUID();
        Mockito.when(authTokenGenerator.generate()).thenReturn("x");

        interceptor.addRule(new Rule.Builder()
                .get()
                .respond(
                        "{\"_embedded\":{\"documents\":[{\"_links\":{\"self\":{\"href\":\"http://success.com/1\"}}}]}}"
                ));
        assertThrows(DocumentTaskProcessingException.class, () ->
            dmStoreDownloader.downloadFile(dmStoreDocId.toString()));
    }

    @Test
    void testThrowNewDocumentTaskProcessingException() {
        UUID dmStoreDocId = UUID.randomUUID();
        Mockito.when(authTokenGenerator.generate()).thenReturn("x");

        interceptor.addRule(new Rule.Builder()
                .get()
                .respond("").code(500));
        assertThrows(DocumentTaskProcessingException.class, () ->
            dmStoreDownloader.downloadFile(dmStoreDocId.toString()));
    }

}
