package uk.gov.hmcts.reform.em.npa.service.impl;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.em.npa.service.DmStoreDownloader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
@Transactional
public class DmStoreDownloaderImpl implements DmStoreDownloader {

    private OkHttpClient client = new OkHttpClient();

    @Override
    public File downloadFile(String id) {

        String token = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJlbV9ndyIsImV4cCI6MTUzNzM4Mzg1M30.xiAPo0CLDxnD9JuZvwfUn3paZyazbb7c8v1N5xu-UaBcynUKAlDUeHYa_pS4MiFpOF-tm2XAEC6d5QCMfJkM7g";

        Request request = new Request.Builder()
            .addHeader("user-roles", "caseworker")
            .addHeader("ServiceAuthorization", token)
            .url("http://localhost:4603/documents/" + id + "/binary")
            .build();

        try {
            Response response = client.newCall(request).execute();

            Path tempPath = Paths.get(System.getProperty("java.io.tmpdir") + "/" + id + ".pdf");

            try {
                Files.copy(response.body().byteStream(), tempPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new DocumentTaskProcessingException("Could not copy the file to a temp location", e);
            }

            return tempPath.toFile();

        } catch (IOException e) {
            throw new DocumentTaskProcessingException("Could not access the binary", e);
        }

    }

}
