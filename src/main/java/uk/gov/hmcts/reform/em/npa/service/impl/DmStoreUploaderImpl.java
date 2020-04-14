package uk.gov.hmcts.reform.em.npa.service.impl;

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

    public DmStoreUploaderImpl(OkHttpClient okHttpClient, AuthTokenGenerator authTokenGenerator,
                               @Value("${dm-store-app.base-url}") String dmStoreAppBaseUrl,
                               SecurityUtils securityUtils) {
        this.okHttpClient = okHttpClient;
        this.authTokenGenerator = authTokenGenerator;
        this.dmStoreAppBaseUrl = dmStoreAppBaseUrl;
        this.securityUtils = securityUtils;
    }

    @Override
    public void uploadFile(File file, DocumentTask documentTask) throws DocumentTaskProcessingException {
        if (documentTask.getOutputDocumentId() != null) {
            uploadNewDocumentVersion(file, documentTask.getOutputDocumentId());
        } else {
            uploadNewDocument(file, documentTask);
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

    @Override
    public void uploadNewDocumentVersion(File file, String documentId) throws DocumentTaskProcessingException {

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
                    .url(dmStoreAppBaseUrl + dmStoreUploadEndpoint + "/" + documentId)
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
