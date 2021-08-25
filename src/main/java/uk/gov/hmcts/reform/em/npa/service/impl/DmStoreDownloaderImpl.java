package uk.gov.hmcts.reform.em.npa.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.document.am.feign.CaseDocumentClientApi;
import uk.gov.hmcts.reform.ccd.document.am.model.Document;
import uk.gov.hmcts.reform.em.npa.service.DmStoreDownloader;
import uk.gov.hmcts.reform.em.npa.service.exception.DocumentTaskProcessingException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;


@Service
@Transactional
public class DmStoreDownloaderImpl implements DmStoreDownloader {

    private final Logger log = LoggerFactory.getLogger(DmStoreDownloaderImpl.class);

    private final OkHttpClient okHttpClient;

    private final AuthTokenGenerator authTokenGenerator;

    private String dmStoreAppBaseUrl;

    private final String dmStoreDownloadEndpoint = "/documents/";

    private final ObjectMapper objectMapper;

    @Autowired
    private CaseDocumentClientApi caseDocumentClientApi;

    public DmStoreDownloaderImpl(OkHttpClient okHttpClient, AuthTokenGenerator authTokenGenerator,
                                 @Value("${document_management.base-url}") String dmStoreAppBaseUrl,
                                 ObjectMapper objectMapper) {
        this.okHttpClient = okHttpClient;
        this.authTokenGenerator = authTokenGenerator;
        this.dmStoreAppBaseUrl = dmStoreAppBaseUrl;
        this.objectMapper = objectMapper;
    }


    @Override
    public File downloadFile(String id) throws DocumentTaskProcessingException {

        try {

            Response response = getDocumentStoreResponse(dmStoreAppBaseUrl + dmStoreDownloadEndpoint + id);

            if (response.isSuccessful()) {
                JsonNode documentMetaData = objectMapper.readTree(response.body().byteStream());

                log.debug("Accessing binary of the DM document: {}",
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(documentMetaData));

                String documentBinaryUrl = new StringBuffer()
                                                .append(dmStoreAppBaseUrl)
                                                    .append(dmStoreDownloadEndpoint)
                                                    .append(id)
                                                    .append("/binary").toString();

                String originalDocumentName = documentMetaData.get("originalDocumentName").asText();
                String fileType = FilenameUtils.getExtension(originalDocumentName);

                log.debug("Accessing documentBinaryUrl: {}", documentBinaryUrl);

                Response binaryResponse = getDocumentStoreResponse(documentBinaryUrl);

                return copyResponseToFile(binaryResponse.body().byteStream(), fileType);

            } else {
                throw new DocumentTaskProcessingException("Could not access the binary. HTTP response: " + response.code());
            }

        } catch (RuntimeException | IOException e) {
            throw new DocumentTaskProcessingException(String.format("Could not access the binary: %s", e.getMessage()), e);
        }

    }

    @Override
    public File downloadFile(String auth, String serviceAuth, UUID documentId) throws IOException, DocumentTaskProcessingException {

        ResponseEntity<Resource> response =  caseDocumentClientApi.getDocumentBinary(auth, serviceAuth, documentId);

        if (Objects.nonNull(response.getBody())) {

            Document document = caseDocumentClientApi.getMetadataForDocument(auth, serviceAuth, documentId);
            String originalDocumentName = document.originalDocumentName;
            String fileType = FilenameUtils.getExtension(originalDocumentName);

            ByteArrayResource resource = (ByteArrayResource) response.getBody();

            if (Objects.nonNull(resource)) {
                return copyResponseToFile(resource.getInputStream(), fileType);
            }
        }

        throw new DocumentTaskProcessingException("Could not access the binary. HTTP response: " + response.getStatusCode());
    }

    private Response getDocumentStoreResponse(String documentUri) throws IOException {

        log.info("getDocumentStoreResponse - URL: {}", documentUri);

        return okHttpClient.newCall(new Request.Builder()
            .addHeader("user-roles", "caseworker")
            .addHeader("ServiceAuthorization", authTokenGenerator.generate())
            .url(documentUri)
            .build()).execute();
    }

    private File copyResponseToFile(InputStream inputStream, String fileType) throws DocumentTaskProcessingException {
        try {

            File tempFile = File.createTempFile("dm-store", "."+fileType);
            Files.copy(inputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            return tempFile;
        } catch (IOException e) {
            throw new DocumentTaskProcessingException("Could not copy the file to a temp location", e);
        }
    }

}
