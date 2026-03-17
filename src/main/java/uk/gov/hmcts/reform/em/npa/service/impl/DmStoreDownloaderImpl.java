package uk.gov.hmcts.reform.em.npa.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.em.npa.service.DmStoreDownloader;
import uk.gov.hmcts.reform.em.npa.service.exception.DocumentTaskProcessingException;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

@Service
@Transactional
public class DmStoreDownloaderImpl implements DmStoreDownloader {

    private final Logger log = LoggerFactory.getLogger(DmStoreDownloaderImpl.class);

    private final OkHttpClient okHttpClient;

    private final AuthTokenGenerator authTokenGenerator;

    private final IdamClient idamClient;

    private String dmStoreAppBaseUrl;

    private static final String DM_STORE_DOWNLOAD_ENDPOINT = "/documents/";

    private final ObjectMapper objectMapper;

    public DmStoreDownloaderImpl(OkHttpClient okHttpClient, AuthTokenGenerator authTokenGenerator,
                                 IdamClient idamClient,
                                 @Value("${document_management.base-url}") String dmStoreAppBaseUrl,
                                 ObjectMapper objectMapper) {
        this.okHttpClient = okHttpClient;
        this.authTokenGenerator = authTokenGenerator;
        this.idamClient = idamClient;
        this.dmStoreAppBaseUrl = dmStoreAppBaseUrl;
        this.objectMapper = objectMapper;
    }

    @Override
    public File downloadFile(String id, String userToken) throws DocumentTaskProcessingException {

        try {

            UserInfo userInfo = idamClient.getUserInfo(userToken);
            String userId = userInfo.getUid();
            String userRoles = String.join(",", userInfo.getRoles());

            Response response = getDocumentStoreResponse(
                dmStoreAppBaseUrl + DM_STORE_DOWNLOAD_ENDPOINT + id, userId, userRoles);

            if (response.isSuccessful()) {
                JsonNode documentMetaData = objectMapper.readTree(response.body().byteStream());

                log.atInfo()
                    .setMessage("Accessing binary of the DM document: {}")
                    .addArgument(() -> {
                        try {
                            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(documentMetaData);
                        } catch (JsonProcessingException e) {
                            return "Error serializing document metadata: " + e.getMessage();
                        }
                    })
                    .log();

                String documentBinaryUrl = new StringBuffer()
                    .append(dmStoreAppBaseUrl)
                    .append(DM_STORE_DOWNLOAD_ENDPOINT)
                    .append(id)
                    .append("/binary").toString();

                String originalDocumentName = documentMetaData.get("originalDocumentName").asText();
                String fileType = FilenameUtils.getExtension(originalDocumentName);

                log.info("Accessing documentBinaryUrl: {}", documentBinaryUrl);

                Response binaryResponse = getDocumentStoreResponse(documentBinaryUrl, userId, userRoles);

                return copyResponseToFile(binaryResponse, fileType);

            } else {
                throw new DocumentTaskProcessingException(
                    "Could not access the binary. HTTP response: " + response.code()
                );
            }

        } catch (RuntimeException | IOException e) {
            throw new DocumentTaskProcessingException(String.format(
                "Could not access the binary: %s", e.getMessage()),
                e
            );
        }

    }

    private Response getDocumentStoreResponse(String documentUri, String userId, String userRoles) throws IOException {

        log.info("getDocumentStoreResponse - URL: {}", documentUri);

        return okHttpClient.newCall(new Request.Builder()
            .addHeader("user-id", userId)
            .addHeader("user-roles", userRoles)
            .addHeader("ServiceAuthorization", authTokenGenerator.generate())
            .url(documentUri)
            .build()).execute();
    }

    private File copyResponseToFile(Response response, String fileType) throws DocumentTaskProcessingException {
        try {

            File tempFile = File.createTempFile("dm-store", "." + fileType);
            Files.copy(response.body().byteStream(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            return tempFile;
        } catch (IOException e) {
            throw new DocumentTaskProcessingException("Could not copy the file to a temp location", e);
        }
    }

}