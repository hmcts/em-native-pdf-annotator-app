package uk.gov.hmcts.reform.em.npa.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.em.npa.config.Constants;
import uk.gov.hmcts.reform.em.npa.config.security.SecurityUtils;
import uk.gov.hmcts.reform.em.npa.service.exception.DocumentTaskProcessingException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DmStoreUploaderImplTest {

    @Mock
    private OkHttpClient okHttpClient;

    @Mock
    private AuthTokenGenerator authTokenGenerator;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private Call call;

    @Mock
    private ResponseBody responseBody;

    private ObjectMapper objectMapper;

    private DmStoreUploaderImpl dmStoreUploader;

    private static final String DM_STORE_BASE_URL = "http://dm-store:8080";
    private static final String AUTH_TOKEN = "Bearer test-token";
    private static final String USER_ID = "test-user-123";

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        dmStoreUploader = new DmStoreUploaderImpl(
            okHttpClient,
            authTokenGenerator,
            DM_STORE_BASE_URL,
            securityUtils,
            objectMapper
        );
    }

    private void setupDefaultMocks() {
        when(authTokenGenerator.generate()).thenReturn(AUTH_TOKEN);
        when(securityUtils.getCurrentUserLogin()).thenReturn(Optional.of(USER_ID));
    }

    @Test
    void shouldUploadDocumentSuccessfully() throws Exception {
        setupDefaultMocks();
        File testFile = createTestFile("test.pdf", "PDF content");
        String responseJson = """
            {
              "_embedded": {
                "documents": [
                  {
                    "_links": {
                      "self": {
                        "href": "http://dm-store:8080/documents/doc-123"
                      }
                    }
                  }
                ]
              }
            }
            """;

        Response response = new Response.Builder()
            .request(new Request.Builder().url(DM_STORE_BASE_URL + "/documents").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(ResponseBody.create(responseJson, okhttp3.MediaType.get("application/json")))
            .build();

        when(okHttpClient.newCall(any(Request.class))).thenReturn(call);
        when(call.execute()).thenReturn(response);

        JsonNode result = dmStoreUploader.uploadDocument(testFile);

        assertThat(result).isNotNull();
        assertThat(result.at("/_embedded/documents/0/_links/self/href").asText())
            .isEqualTo("http://dm-store:8080/documents/doc-123");

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(okHttpClient).newCall(requestCaptor.capture());

        Request capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.url().toString()).isEqualTo(DM_STORE_BASE_URL + "/documents");
        assertThat(capturedRequest.method()).isEqualTo("POST");
        assertThat(capturedRequest.header("user-id")).isEqualTo(USER_ID);
        assertThat(capturedRequest.header("user-roles")).isEqualTo("caseworker");
        assertThat(capturedRequest.header("ServiceAuthorization")).isEqualTo(AUTH_TOKEN);
    }

    @Test
    void shouldUploadDocumentWithAnonymousUserWhenNoUserLogin() throws Exception {
        when(authTokenGenerator.generate()).thenReturn(AUTH_TOKEN);
        when(securityUtils.getCurrentUserLogin()).thenReturn(Optional.empty());

        File testFile = createTestFile("test.pdf", "PDF content");
        String responseJson = """
            {
              "_embedded": {
                "documents": []
              }
            }
            """;

        Response response = new Response.Builder()
            .request(new Request.Builder().url(DM_STORE_BASE_URL + "/documents").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(ResponseBody.create(responseJson, okhttp3.MediaType.get("application/json")))
            .build();

        when(okHttpClient.newCall(any(Request.class))).thenReturn(call);
        when(call.execute()).thenReturn(response);

        dmStoreUploader.uploadDocument(testFile);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(okHttpClient).newCall(requestCaptor.capture());

        Request capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.header("user-id")).isEqualTo(Constants.ANONYMOUS_USER);
    }

    @Test
    void shouldThrowExceptionWhenResponseIsNotSuccessful() throws Exception {
        setupDefaultMocks();
        File testFile = createTestFile("test.pdf", "PDF content");

        Response response = new Response.Builder()
            .request(new Request.Builder().url(DM_STORE_BASE_URL + "/documents").build())
            .protocol(Protocol.HTTP_1_1)
            .code(500)
            .message("Internal Server Error")
            .body(ResponseBody.create("Error", okhttp3.MediaType.get("text/plain")))
            .build();

        when(okHttpClient.newCall(any(Request.class))).thenReturn(call);
        when(call.execute()).thenReturn(response);

        assertThatThrownBy(() -> dmStoreUploader.uploadDocument(testFile))
            .isInstanceOf(DocumentTaskProcessingException.class)
            .hasMessageContaining("Couldn't upload the file. Response code: 500");
    }

    @Test
    void shouldThrowExceptionWhenResponseIs404() throws Exception {
        setupDefaultMocks();
        File testFile = createTestFile("test.pdf", "PDF content");

        Response response = new Response.Builder()
            .request(new Request.Builder().url(DM_STORE_BASE_URL + "/documents").build())
            .protocol(Protocol.HTTP_1_1)
            .code(404)
            .message("Not Found")
            .body(ResponseBody.create("Not Found", okhttp3.MediaType.get("text/plain")))
            .build();

        when(okHttpClient.newCall(any(Request.class))).thenReturn(call);
        when(call.execute()).thenReturn(response);

        assertThatThrownBy(() -> dmStoreUploader.uploadDocument(testFile))
            .isInstanceOf(DocumentTaskProcessingException.class)
            .hasMessageContaining("Couldn't upload the file. Response code: 404");
    }

    @Test
    void shouldThrowExceptionWhenIOExceptionOccurs() throws Exception {
        setupDefaultMocks();
        File testFile = createTestFile("test.pdf", "PDF content");

        when(okHttpClient.newCall(any(Request.class))).thenReturn(call);
        when(call.execute()).thenThrow(new IOException("Network error"));

        assertThatThrownBy(() -> dmStoreUploader.uploadDocument(testFile))
            .isInstanceOf(DocumentTaskProcessingException.class)
            .hasMessageContaining("Couldn't upload the file:  Network error")
            .hasCauseInstanceOf(IOException.class);
    }

    @Test
    void shouldThrowExceptionWhenRuntimeExceptionOccurs() throws Exception {
        setupDefaultMocks();
        File testFile = createTestFile("test.pdf", "PDF content");

        when(okHttpClient.newCall(any(Request.class))).thenReturn(call);
        when(call.execute()).thenThrow(new RuntimeException("Unexpected error"));

        assertThatThrownBy(() -> dmStoreUploader.uploadDocument(testFile))
            .isInstanceOf(DocumentTaskProcessingException.class)
            .hasMessageContaining("Couldn't upload the file:  Unexpected error")
            .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldThrowExceptionWhenFileDoesNotExist() {
        File nonExistentFile = new File("/non/existent/file.pdf");

        assertThatThrownBy(() -> dmStoreUploader.uploadDocument(nonExistentFile))
            .isInstanceOf(DocumentTaskProcessingException.class)
            .hasMessageContaining("Couldn't upload the file:");
    }

    @Test
    void shouldDetectCorrectMimeTypeForPdfFile() throws Exception {
        setupDefaultMocks();
        File pdfFile = createTestFile("test.pdf", "%PDF-1.4 test content");
        String responseJson = """
            {
              "_embedded": {
                "documents": []
              }
            }
            """;

        Response response = new Response.Builder()
            .request(new Request.Builder().url(DM_STORE_BASE_URL + "/documents").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(ResponseBody.create(responseJson, okhttp3.MediaType.get("application/json")))
            .build();

        when(okHttpClient.newCall(any(Request.class))).thenReturn(call);
        when(call.execute()).thenReturn(response);

        dmStoreUploader.uploadDocument(pdfFile);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(okHttpClient).newCall(requestCaptor.capture());

        Request capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.body()).isNotNull();
        assertThat(capturedRequest.body().contentType()).isNotNull();
    }

    @Test
    void shouldDetectCorrectMimeTypeForTextFile() throws Exception {
        setupDefaultMocks();
        File textFile = createTestFile("test.txt", "Plain text content");
        String responseJson = """
            {
              "_embedded": {
                "documents": []
              }
            }
            """;

        Response response = new Response.Builder()
            .request(new Request.Builder().url(DM_STORE_BASE_URL + "/documents").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(ResponseBody.create(responseJson, okhttp3.MediaType.get("application/json")))
            .build();

        when(okHttpClient.newCall(any(Request.class))).thenReturn(call);
        when(call.execute()).thenReturn(response);

        dmStoreUploader.uploadDocument(textFile);

        verify(okHttpClient).newCall(any(Request.class));
    }

    @Test
    void shouldIncludeClassificationInRequest() throws Exception {
        setupDefaultMocks();
        File testFile = createTestFile("test.pdf", "PDF content");
        String responseJson = """
            {
              "_embedded": {
                "documents": []
              }
            }
            """;

        Response response = new Response.Builder()
            .request(new Request.Builder().url(DM_STORE_BASE_URL + "/documents").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(ResponseBody.create(responseJson, okhttp3.MediaType.get("application/json")))
            .build();

        when(okHttpClient.newCall(any(Request.class))).thenReturn(call);
        when(call.execute()).thenReturn(response);

        dmStoreUploader.uploadDocument(testFile);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(okHttpClient).newCall(requestCaptor.capture());

        Request capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.body()).isNotNull();
        assertThat(capturedRequest.body().contentType().toString()).contains("multipart/form-data");
    }

    private File createTestFile(String filename, String content) throws IOException {
        Path filePath = tempDir.resolve(filename);
        Files.writeString(filePath, content);
        return filePath.toFile();
    }
}
