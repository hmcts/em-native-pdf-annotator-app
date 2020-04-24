package uk.gov.hmcts.reform.em.npa.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.em.npa.config.Constants;
import uk.gov.hmcts.reform.em.npa.config.security.SecurityUtils;
import uk.gov.hmcts.reform.em.npa.domain.DocumentTask;
import uk.gov.hmcts.reform.em.npa.service.DmStoreUploader;

import java.io.File;
import java.io.IOException;

@Service
@Transactional
public class DmStoreUploaderImpl implements DmStoreUploader {

    private final OkHttpClient okHttpClient;

    private final AuthTokenGenerator authTokenGenerator;

    private final String dmStoreAppBaseUrl;

    private final String dmStoreUploadEndpoint = "/documents";

    private final SecurityUtils securityUtils;

    private final ObjectMapper objectMapper;

    public DmStoreUploaderImpl(OkHttpClient okHttpClient, AuthTokenGenerator authTokenGenerator,
                               @Value("${dm-store-app.base-url}") String dmStoreAppBaseUrl,
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
    public JsonNode uploadNewDocument(File file) throws DocumentTaskProcessingException {

        // DRY ?
        try {

            MultipartBody requestBody = new MultipartBody
                .Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("classification", "PUBLIC")
                .addFormDataPart("files", file.getName(), RequestBody.create(MediaType.get("application/pdf"), file))
                .build();

            Request request = new Request.Builder()
                .addHeader("user-id", securityUtils.getCurrentUserLogin().orElse(Constants.ANONYMOUS_USER))
                .addHeader("user-roles", "caseworker")
                .addHeader("ServiceAuthorization", authTokenGenerator.generate())
                .url(dmStoreAppBaseUrl + dmStoreUploadEndpoint)
                .method("POST", requestBody)
                .build();

            Response response = okHttpClient.newCall(request).execute();

            if (response.isSuccessful()) {

                return objectMapper.readTree(response.body().string());

            } else {
                throw new DocumentTaskProcessingException("Couldn't upload the file. Response code: " + response.code(), null);
            }

        } catch (RuntimeException | IOException e) {
            throw new DocumentTaskProcessingException(String.format("Couldn't upload the file:  %s", e.getMessage()), e);
        }

    }

    private void uploadNewDocument(File file, DocumentTask documentTask) throws DocumentTaskProcessingException {

        try {

            MultipartBody requestBody = new MultipartBody
                    .Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("classification", "PUBLIC")
                    .addFormDataPart("files", file.getName(), RequestBody.create(MediaType.get("application/pdf"), file))
                    .build();

            Request request = new Request.Builder()
                    .addHeader("user-id", securityUtils.getCurrentUserLogin().orElse(Constants.ANONYMOUS_USER))
                    .addHeader("user-roles", "caseworker")
                    .addHeader("ServiceAuthorization", authTokenGenerator.generate())
                    .url(dmStoreAppBaseUrl + dmStoreUploadEndpoint)
                    .method("POST", requestBody)
                    .build();

            Response response = okHttpClient.newCall(request).execute();

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
                throw new DocumentTaskProcessingException("Couldn't upload the file. Response code: " + response.code(), null);
            }

        } catch (RuntimeException | IOException e) {
            throw new DocumentTaskProcessingException(String.format("Couldn't upload the file:  %s", e.getMessage()), e);
        }
    }

    private void uploadNewDocumentVersion(File file, DocumentTask documentTask) throws DocumentTaskProcessingException {

        try {

            MultipartBody requestBody = new MultipartBody
                    .Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", file.getName(), RequestBody.create(MediaType.get("application/pdf"), file))
                    .build();

            Request request = new Request.Builder()
                    .addHeader("user-id", securityUtils.getCurrentUserLogin().orElse(Constants.ANONYMOUS_USER))
                    .addHeader("user-roles", "caseworker")
                    .addHeader("ServiceAuthorization", authTokenGenerator.generate())
                    .url(dmStoreAppBaseUrl + dmStoreUploadEndpoint + "/" + documentTask.getOutputDocumentId())
                    .method("POST", requestBody)
                    .build();

            Response response = okHttpClient.newCall(request).execute();

            if (!response.isSuccessful()) {
                throw new DocumentTaskProcessingException("Couldn't upload the file. Response code: " + response.code(), null);
            }

        } catch (RuntimeException | IOException e) {
            throw new DocumentTaskProcessingException("Couldn't upload the file", e);
        }
    }

}
