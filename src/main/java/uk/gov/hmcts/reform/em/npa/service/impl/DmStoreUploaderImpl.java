package uk.gov.hmcts.reform.em.npa.service.impl;

import okhttp3.*;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.em.npa.service.DmStoreUploader;

import java.io.File;
import java.io.IOException;

@Service
@Transactional
public class DmStoreUploaderImpl implements DmStoreUploader {

    private OkHttpClient client = new OkHttpClient();

    @Override
    public String uploadFile(File file) {

        String token = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJlbV9ndyIsImV4cCI6MTUzNzM4Mzg1M30.xiAPo0CLDxnD9JuZvwfUn3paZyazbb7c8v1N5xu-UaBcynUKAlDUeHYa_pS4MiFpOF-tm2XAEC6d5QCMfJkM7g";

        MultipartBody requestBody = new MultipartBody
            .Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("classification", "PUBLIC")
            .addFormDataPart("files", file.getName(), RequestBody.create(MediaType.get("application/pdf"), file))
            .build();

        Request request = new Request.Builder()
            .addHeader("user-roles", "caseworker")
            .addHeader("ServiceAuthorization", token)
            .addHeader("Content-Type", "application/vnd.uk.gov.hmcts.dm.document-collection.v1+hal+json;charset=UTF-8")
            .url("http://localhost:4603/documents")
            .method("POST", requestBody)
            .build();

        try {

            Response response = client.newCall(request).execute();

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

                return split[split.length - 1];

            } else {
                throw new DocumentTaskProcessingException("Couldn't upload the file. Response" + response.toString(), null);
            }

        } catch (IOException e) {
            throw new DocumentTaskProcessingException("Couldn't upload the file", e);
        }

    }
}
