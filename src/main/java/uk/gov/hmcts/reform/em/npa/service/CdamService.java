package uk.gov.hmcts.reform.em.npa.service;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.document.am.feign.CaseDocumentClientApi;
import uk.gov.hmcts.reform.em.npa.service.exception.DocumentTaskProcessingException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

@Service
public class CdamService {

    private final CaseDocumentClientApi caseDocumentClientApi;

    @Autowired
    public CdamService(CaseDocumentClientApi caseDocumentClientApi) {
        this.caseDocumentClientApi = caseDocumentClientApi;
    }

    public File downloadFile(String auth, String serviceAuth, UUID documentId) throws
            IOException, DocumentTaskProcessingException {

        ResponseEntity<Resource> response =  caseDocumentClientApi.getDocumentBinary(auth, serviceAuth, documentId);
        HttpStatusCode status = null;

        if (Objects.nonNull(response)) {
            status = response.getStatusCode();
            var byteArrayResource = (ByteArrayResource) response.getBody();
            if (Objects.nonNull(byteArrayResource)) {
                try (var inputStream = byteArrayResource.getInputStream()) {
                    var document = caseDocumentClientApi.getMetadataForDocument(auth, serviceAuth, documentId);
                    var originalDocumentName = document.originalDocumentName;
                    var fileType = FilenameUtils.getExtension(originalDocumentName);
                    return copyResponseToFile(inputStream, fileType);
                }
            }
        }

        throw new DocumentTaskProcessingException(String.format("Could not access the binary. HTTP response: %s",
                status));
    }

    private File copyResponseToFile(InputStream inputStream, String fileType) throws DocumentTaskProcessingException {
        try {

            var tempDir = Files.createTempDirectory("pg-");
            var suffix = fileType.isBlank() ? null : "." + fileType;
            var tempFile = Files.createTempFile(tempDir, "document-", suffix);

            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);

            return tempFile.toFile();
        } catch (IOException e) {
            throw new DocumentTaskProcessingException("Could not copy the file to a temp location", e);
        }
    }

}
