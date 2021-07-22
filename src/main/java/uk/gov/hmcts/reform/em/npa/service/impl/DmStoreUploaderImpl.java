package uk.gov.hmcts.reform.em.npa.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.apache.tika.Tika;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.em.npa.config.Constants;
import uk.gov.hmcts.reform.em.npa.config.security.SecurityUtils;
import uk.gov.hmcts.reform.em.npa.domain.DocumentTask;
import uk.gov.hmcts.reform.em.npa.service.DmStoreUploader;
import uk.gov.hmcts.reform.em.npa.service.exception.DocumentTaskProcessingException;

import java.io.File;
import java.io.IOException;

@Service
@Transactional
public class DmStoreUploaderImpl implements DmStoreUploader {

    public static final String USER_ID = "user-id";
    public static final String USER_ROLES = "user-roles";
    public static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    public static final String CASEWORKER = "caseworker";
    public static final String CLASSIFICATION = "classification";
    public static final String PUBLIC = "PUBLIC";
    public static final String UPLOAD_EXCEPTION = "Couldn't upload the file:  %s";
    public static final String UPLOAD_ERROR = "Couldn't upload the file. Response code: ";
    private final OkHttpClient okHttpClient;

    private final AuthTokenGenerator authTokenGenerator;
    private final String dmStoreAppBaseUrl;
    private final String dmStoreUploadEndpoint = "/documents";
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
    public void uploadFile(File file, DocumentTask documentTask) throws DocumentTaskProcessingException {
        if (documentTask.getOutputDocumentId() != null) {
            uploadNewDocumentVersion(file, documentTask);
        } else {
            uploadNewDocument(file, documentTask);
        }
    }

    @Override
    public JsonNode uploadDocument(File file) throws DocumentTaskProcessingException {
        try {
            Request request = createUploadRequest(file);
            Response response = okHttpClient.newCall(request).execute();
            if (response.isSuccessful()) {
                return objectMapper.readTree(response.body().string());
            } else {
                throw new DocumentTaskProcessingException(UPLOAD_ERROR + response.code(), null);
            }
        } catch (RuntimeException | IOException e) {
            throw new DocumentTaskProcessingException(String.format(UPLOAD_EXCEPTION, e.getMessage()), e);
        }
    }

    private void uploadNewDocument(File file, DocumentTask documentTask) throws DocumentTaskProcessingException {
        try {
            Request uploadRequest = createUploadRequest(file);
            Response response = okHttpClient.newCall(uploadRequest).execute();
            if (response.isSuccessful()) {
                JSONObject jsonObject = new JSONObject(response.body().string());
                String[] split = jsonObject
                        .getJSONObject("_embedded")
                        .getJSONArray("documents")
                        .getJSONObject(0)
                        .getJSONObject("_links")
                        .getJSONObject("self")
                        .getString("href")
                        .split("\\/");
                documentTask.setOutputDocumentId(split[split.length - 1]);
            } else {
                throw new DocumentTaskProcessingException(UPLOAD_ERROR + response.code(), null);
            }
        } catch (RuntimeException | IOException e) {
            throw new DocumentTaskProcessingException(String.format(UPLOAD_EXCEPTION, e.getMessage()), e);
        }
    }

    private void uploadNewDocumentVersion(File file, DocumentTask documentTask) throws DocumentTaskProcessingException {
        try {
            MultipartBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", file.getName(), RequestBody.create(file, getMimeType(file)))
                    .build();
            String url = dmStoreAppBaseUrl + dmStoreUploadEndpoint + "/" + documentTask.getOutputDocumentId();
            Request request = createMultipartRequest(url, requestBody);
            Response response = okHttpClient.newCall(request).execute();
            if (!response.isSuccessful()) {
                throw new DocumentTaskProcessingException(UPLOAD_ERROR + response.code(), null);
            }
        } catch (RuntimeException | IOException e) {
            throw new DocumentTaskProcessingException(String.format(UPLOAD_EXCEPTION, e.getMessage()), e);
        }
    }

    private Request createUploadRequest(File file) throws IOException {
        MultipartBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(CLASSIFICATION, PUBLIC)
                .addFormDataPart("files", file.getName(), RequestBody.create(file, getMimeType(file)))
                .build();
        String url = dmStoreAppBaseUrl + dmStoreUploadEndpoint;
        return createMultipartRequest(url, requestBody);
    }

    private Request createMultipartRequest(String url, MultipartBody requestBody) {
        Request request = new Request.Builder()
                .addHeader(USER_ID, securityUtils.getCurrentUserLogin().orElse(Constants.ANONYMOUS_USER))
                .addHeader(USER_ROLES, CASEWORKER)
                .addHeader(SERVICE_AUTHORIZATION, authTokenGenerator.generate())
                .url(url)
                .method("POST", requestBody)
                .build();
        return request;
    }

    private MediaType getMimeType(File file) throws IOException {
        String mimeType = new Tika().detect(file);
        return MediaType.get(mimeType);
    }
}
