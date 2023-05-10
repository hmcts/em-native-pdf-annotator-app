package uk.gov.hmcts.reform.em.npa.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.em.npa.config.Constants;
import uk.gov.hmcts.reform.em.npa.config.security.SecurityUtils;
import uk.gov.hmcts.reform.em.npa.service.DmStoreUploader;
import uk.gov.hmcts.reform.em.npa.service.exception.DocumentTaskProcessingException;

import java.io.File;
import java.io.IOException;

@Service
@Transactional
public class DmStoreUploaderImpl implements DmStoreUploader {

    private final OkHttpClient okHttpClient;

    private final AuthTokenGenerator authTokenGenerator;

    private final String dmStoreAppBaseUrl;

    private static final String DM_STORE_UPLOAD_ENDPOINT = "/documents";

    private final SecurityUtils securityUtils;

    private final ObjectMapper objectMapper;

    public DmStoreUploaderImpl(OkHttpClient okHttpClient, AuthTokenGenerator authTokenGenerator,
                               @Value("${document_management.base-url}") String dmStoreAppBaseUrl,
                               SecurityUtils securityUtils, ObjectMapper objectMapper) {
        this.okHttpClient = okHttpClient;
        this.authTokenGenerator = authTokenGenerator;
        this.dmStoreAppBaseUrl = dmStoreAppBaseUrl;
        this.securityUtils = securityUtils;
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode uploadDocument(File file) throws DocumentTaskProcessingException {

        // DRY ?
        try {
            String mimeType = getMimeType(file);

            MultipartBody requestBody = new MultipartBody
                .Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("classification", "PUBLIC")
                .addFormDataPart("files", file.getName(), RequestBody.create(file, MediaType.get(mimeType)))
                .build();

            Request request = new Request.Builder()
                .addHeader("user-id", securityUtils.getCurrentUserLogin().orElse(Constants.ANONYMOUS_USER))
                .addHeader("user-roles", "caseworker")
                .addHeader("ServiceAuthorization", authTokenGenerator.generate())
                .url(dmStoreAppBaseUrl + DM_STORE_UPLOAD_ENDPOINT)
                .method("POST", requestBody)
                .build();

            Response response = okHttpClient.newCall(request).execute();

            if (response.isSuccessful()) {

                return objectMapper.readTree(response.body().string());

            } else {
                throw new DocumentTaskProcessingException(
                        "Couldn't upload the file. Response code: " + response.code(),
                        null
                );
            }

        } catch (RuntimeException | IOException e) {
            throw new DocumentTaskProcessingException(String.format(
                    "Couldn't upload the file:  %s", e.getMessage()),
                    e
            );
        }

    }

    private String getMimeType(File file) throws IOException {
        Tika tika = new Tika();
        return tika.detect(file);
    }
}
