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
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.EnumSet;
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
                    var fileName = "document." + fileType;
                    return copyResponseToFile(inputStream, fileName);
                }
            }
        }

        throw new DocumentTaskProcessingException(String.format("Could not access the binary. HTTP response: %s",
                status));
    }

    private File copyResponseToFile(InputStream inputStream, String fileName) throws DocumentTaskProcessingException {
        try {

            var tempDir = Files.createTempDirectory("pg",
                PosixFilePermissions.asFileAttribute(EnumSet.allOf(PosixFilePermission.class)));
            var tempFile = new File(tempDir.toAbsolutePath().toFile(), fileName);

            Files.copy(inputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            return tempFile;
        } catch (IOException e) {
            throw new DocumentTaskProcessingException("Could not copy the file to a temp location", e);
        }
    }

}
