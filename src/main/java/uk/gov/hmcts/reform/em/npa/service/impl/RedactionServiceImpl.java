package uk.gov.hmcts.reform.em.npa.service.impl;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.npa.config.Constants;
import uk.gov.hmcts.reform.em.npa.config.security.SecurityUtils;
import uk.gov.hmcts.reform.em.npa.redaction.ImageRedaction;
import uk.gov.hmcts.reform.em.npa.redaction.PdfRedaction;
import uk.gov.hmcts.reform.em.npa.repository.MarkUpRepository;
import uk.gov.hmcts.reform.em.npa.service.DmStoreDownloader;
import uk.gov.hmcts.reform.em.npa.service.RedactionService;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RedactionDTO;
import uk.gov.hmcts.reform.em.npa.service.exception.DocumentTaskProcessingException;
import uk.gov.hmcts.reform.em.npa.service.exception.FileTypeException;
import uk.gov.hmcts.reform.em.npa.service.exception.RedactionProcessingException;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Service
public class RedactionServiceImpl implements RedactionService {

    private final Logger log = LoggerFactory.getLogger(RedactionServiceImpl.class);

    private DmStoreDownloader dmStoreDownloader;
    private PdfRedaction pdfRedaction;
    private ImageRedaction imageRedaction;
    private MarkUpRepository markUpRepository;
    private SecurityUtils securityUtils;

    @Value("#{'${redaction.multipart.image-ext}'.split(',')}")
    List<String> imageExtensionsList;

    public RedactionServiceImpl (DmStoreDownloader dmStoreDownloader, PdfRedaction pdfRedaction, ImageRedaction imageRedaction,
                                 MarkUpRepository markUpRepository, SecurityUtils securityUtils) {
        this.dmStoreDownloader = dmStoreDownloader;
        this.pdfRedaction = pdfRedaction;
        this.imageRedaction = imageRedaction;
        this.markUpRepository = markUpRepository;
        this.securityUtils = securityUtils;
    }

    @Override
    public File redactFile(String jwt, String caseId, UUID documentId, List<RedactionDTO> redactionDTOList) {
        try {
            File originalFile = dmStoreDownloader.downloadFile(documentId.toString());
            String fileType = FilenameUtils.getExtension(originalFile.getName());

            File updatedFile;
            if (fileType.equals("pdf")) {
                log.info("Applying redaction to PDF file");
                updatedFile = pdfRedaction.redactPdf(originalFile, redactionDTOList);
            } else if (imageExtensionsList.contains(fileType)) {
                log.info("Applying redaction to Image Document");
                updatedFile = imageRedaction.redactImage(originalFile, redactionDTOList.get(0).getRectangles());
            } else {
                throw new FileTypeException("Redaction cannot be applied to the file type provided");
            }
            markUpRepository.deleteAllByDocumentIdAndCreatedBy(documentId, securityUtils.getCurrentUserLogin().orElse(Constants.ANONYMOUS_USER));
            return updatedFile;
        } catch (DocumentTaskProcessingException e) {
            log.error(e.getMessage(), e);
            throw new RedactionProcessingException("Error processing Redaction Task");
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new FileTypeException("File processing error encountered");
        }
    }
}