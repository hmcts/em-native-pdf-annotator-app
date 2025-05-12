package uk.gov.hmcts.reform.em.npa.service.impl;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.em.npa.service.exception.DocumentTaskProcessingException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DmStoreDownloaderImplTest {

    @Mock
    private OkHttpClient okHttpClient;

    @Mock
    private AuthTokenGenerator authTokenGenerator;

    @Mock
    private Call mockCall;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private DmStoreDownloaderImpl dmStoreDownloader;

    @Captor
    private ArgumentCaptor<Request> requestCaptor;

    private static final String DM_STORE_BASE_URL = "http://localhost:4603";
    private static final String S2S_TOKEN = "s2s-token";
    private static final String USER_ROLES = "caseworker";
    private static final String DOC_ID = UUID.randomUUID().toString();
    private static final String METADATA_URL = DM_STORE_BASE_URL + "/documents/" + DOC_ID;
    private static final String BINARY_URL = METADATA_URL + "/binary";
    private static final String ORIGINAL_DOC_NAME = "test_document.pdf";
    private static final String METADATA_JSON = "{\"originalDocumentName\":\"" + ORIGINAL_DOC_NAME + "\"}";
    private static final String BINARY_CONTENT = "binary file content";

    private File downloadedFile;

    @BeforeEach
    void setUp() {

        dmStoreDownloader = new DmStoreDownloaderImpl(
            okHttpClient,
            authTokenGenerator,
            DM_STORE_BASE_URL,
            objectMapper
        );

        when(authTokenGenerator.generate()).thenReturn(S2S_TOKEN);
        lenient().when(okHttpClient.newCall(any(Request.class))).thenReturn(mockCall);

        // Set log level to DEBUG to cover debug logging statements
        Logger logger = (Logger) LoggerFactory.getLogger(DmStoreDownloaderImpl.class);
        logger.setLevel(Level.DEBUG);
    }

    @AfterEach
    void tearDown() throws IOException {

        if (downloadedFile != null && downloadedFile.exists()) {
            Files.deleteIfExists(downloadedFile.toPath());
            downloadedFile = null;
        }
    }

    @Test
    void shouldDownloadFileSuccessfully() throws IOException, DocumentTaskProcessingException {

        Response metadataResponse = createSuccessfulResponse(METADATA_JSON);
        Response binaryResponse = createSuccessfulResponse(BINARY_CONTENT);

        when(mockCall.execute())
            .thenReturn(metadataResponse)
            .thenReturn(binaryResponse);

        downloadedFile = dmStoreDownloader.downloadFile(DOC_ID);

        assertNotNull(downloadedFile);
        assertTrue(downloadedFile.exists());
        assertTrue(downloadedFile.getName().endsWith(".pdf"));
        assertEquals(BINARY_CONTENT, Files.readString(downloadedFile.toPath()));

        verify(authTokenGenerator, times(2)).generate();
        verify(okHttpClient, times(2)).newCall(requestCaptor.capture());
        verify(mockCall, times(2)).execute();

        Request metaRequest = requestCaptor.getAllValues().get(0);
        assertEquals(METADATA_URL, metaRequest.url().toString());
        assertEquals(S2S_TOKEN, metaRequest.header("ServiceAuthorization"));
        assertEquals(USER_ROLES, metaRequest.header("user-roles"));

        Request binaryRequest = requestCaptor.getAllValues().get(1);
        assertEquals(BINARY_URL, binaryRequest.url().toString());
        assertEquals(S2S_TOKEN, binaryRequest.header("ServiceAuthorization"));
        assertEquals(USER_ROLES, binaryRequest.header("user-roles"));
    }


    @Test
    void shouldThrowExceptionWhenMetadataRequestFails() throws IOException {

        Response response = org.mockito.Mockito.mock(Response.class);
        when(response.isSuccessful()).thenReturn(false);
        when(response.code()).thenReturn(404);

        when(mockCall.execute()).thenReturn(response);

        DocumentTaskProcessingException exception = assertThrows(
            DocumentTaskProcessingException.class,
            () -> dmStoreDownloader.downloadFile(DOC_ID)
        );

        assertThat(exception.getMessage()).contains("Could not access the binary. HTTP response: 404");
        verify(authTokenGenerator, times(1)).generate();
        verify(okHttpClient, times(1)).newCall(any());
        verify(mockCall, times(1)).execute();
    }

    @Test
    void shouldThrowExceptionWhenMetadataJsonIsInvalid() throws IOException {

        String invalidJson = "this is not json";
        Response metadataResponse = createSuccessfulResponse(invalidJson);
        when(mockCall.execute()).thenReturn(metadataResponse);

        DocumentTaskProcessingException exception = assertThrows(
            DocumentTaskProcessingException.class,
            () -> dmStoreDownloader.downloadFile(DOC_ID)
        );

        assertThat(exception.getMessage()).contains("Could not access the binary:");
        assertThat(exception.getCause()).isInstanceOf(JsonProcessingException.class);
    }

    @Test
    void shouldThrowExceptionWhenIOExceptionOccursDuringMetadataRequest() throws IOException {

        IOException ioException = new IOException("Network Error");
        when(mockCall.execute()).thenThrow(ioException);

        DocumentTaskProcessingException exception = assertThrows(
            DocumentTaskProcessingException.class,
            () -> dmStoreDownloader.downloadFile(DOC_ID)
        );

        assertThat(exception.getMessage()).contains("Could not access the binary: Network Error");
        assertThat(exception.getCause()).isSameAs(ioException);
        verify(authTokenGenerator, times(1)).generate();
        verify(okHttpClient, times(1)).newCall(any());
    }

    @Test
    void shouldThrowExceptionWhenIOExceptionOccursDuringBinaryRequest() throws IOException {

        Response metadataResponse = createSuccessfulResponse(METADATA_JSON);

        IOException ioException = new IOException("Binary Network Error");
        when(mockCall.execute())
            .thenReturn(metadataResponse)
            .thenThrow(ioException);

        DocumentTaskProcessingException exception = assertThrows(
            DocumentTaskProcessingException.class,
            () -> dmStoreDownloader.downloadFile(DOC_ID)
        );

        assertThat(exception.getMessage()).contains("Could not access the binary: Binary Network Error");
        assertThat(exception.getCause()).isSameAs(ioException);
    }

    @Test
    void shouldThrowExceptionWhenIOExceptionOccursDuringFileCopy() throws IOException {

        Response metadataResponse = createSuccessfulResponse(METADATA_JSON);
        Response binaryResponse = createSuccessfulResponse(BINARY_CONTENT);
        ResponseBody mockBinaryBody = binaryResponse.body();

        InputStream errorStream = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("Disk full error simulation");
            }
        };
        when(mockBinaryBody.byteStream()).thenReturn(errorStream);

        when(mockCall.execute())
            .thenReturn(metadataResponse)
            .thenReturn(binaryResponse);

        DocumentTaskProcessingException exception = assertThrows(
            DocumentTaskProcessingException.class,
            () -> dmStoreDownloader.downloadFile(DOC_ID)
        );

        assertThat(exception.getMessage()).contains("Could not copy the file to a temp location");
        assertThat(exception.getCause()).isInstanceOf(IOException.class);
        assertThat(exception.getCause().getMessage()).isEqualTo("Disk full error simulation");
    }

    @Test
    void shouldThrowExceptionWhenAuthTokenGeneratorThrowsException() {

        RuntimeException runtimeException = new RuntimeException("Auth Error");
        when(authTokenGenerator.generate()).thenThrow(runtimeException);

        DocumentTaskProcessingException exception = assertThrows(
            DocumentTaskProcessingException.class,
            () -> dmStoreDownloader.downloadFile(DOC_ID)
        );

        assertThat(exception.getMessage()).contains("Could not access the binary: Auth Error");
        assertThat(exception.getCause()).isSameAs(runtimeException);
        verify(okHttpClient, times(0)).newCall(any());
    }

    private Response createSuccessfulResponse(String bodyContent) {
        ResponseBody responseBody = ResponseBody.create(
            bodyContent.getBytes(StandardCharsets.UTF_8),
            MediaType.parse("application/json")
        );

        ResponseBody spyBody = org.mockito.Mockito.spy(responseBody);
        when(spyBody.byteStream()).thenReturn(new ByteArrayInputStream(bodyContent.getBytes(StandardCharsets.UTF_8)));

        Response response = org.mockito.Mockito.mock(Response.class);
        lenient().when(response.isSuccessful()).thenReturn(true);
        when(response.body()).thenReturn(spyBody);

        return response;
    }

}