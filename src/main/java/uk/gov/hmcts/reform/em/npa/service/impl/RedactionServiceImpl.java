package uk.gov.hmcts.reform.em.npa.service.impl;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.npa.redaction.ImageRedaction;
import uk.gov.hmcts.reform.em.npa.redaction.PdfRedaction;
import uk.gov.hmcts.reform.em.npa.service.DmStoreDownloader;
import uk.gov.hmcts.reform.em.npa.service.DmStoreUploader;
import uk.gov.hmcts.reform.em.npa.service.RedactionService;
import uk.gov.hmcts.reform.em.npa.service.dto.external.redaction.RedactionDTO;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class RedactionServiceImpl implements RedactionService {

    private final Logger log = LoggerFactory.getLogger(RedactionServiceImpl.class);

    private final DmStoreDownloader dmStoreDownloader;
    private final DmStoreUploader dmStoreUploader;

    public RedactionServiceImpl (DmStoreDownloader dmStoreDownloader, DmStoreUploader dmStoreUploader){
        this.dmStoreDownloader = dmStoreDownloader;
        this.dmStoreUploader = dmStoreUploader;
    }

    @Autowired
    private PdfRedaction pdfRedaction;
    @Autowired
    private ImageRedaction imageRedaction;

    @Value("#{'${redaction.multipart.image-ext}'.split(',')}")
    java.util.List<String> imageExtensionsList;

    @Override
    public String redactFile(UUID documentId, List<RedactionDTO> redactionDTOList) {
        try {
            File originalFile = dmStoreDownloader.downloadFile(documentId.toString());
            String fileType = FilenameUtils.getExtension(originalFile.getName());
            File updatedFile;

            if (fileType.equals("pdf")) {
                log.info("Applying redaction to PDF file");
                updatedFile = pdfRedaction.redaction(originalFile, redactionDTOList);
            } else if (imageExtensionsList.contains(fileType)) {
                log.info("Applying redaction to Image Document");
                updatedFile = imageRedaction.redaction(originalFile, redactionDTOList);
            } else {
                throw new FileTypeException("Redaction cannot be applied to the file type provided");
            }

            dmStoreUploader.uploadNewDocumentVersion(updatedFile, documentId.toString());

        } catch (DocumentTaskProcessingException e) {
            log.error(e.getMessage(), e);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new FileTypeException("File processing error");
        }

        return documentId.toString();
    }
}
