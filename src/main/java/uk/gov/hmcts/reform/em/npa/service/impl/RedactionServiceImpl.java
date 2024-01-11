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
import uk.gov.hmcts.reform.em.npa.service.CdamService;
import uk.gov.hmcts.reform.em.npa.service.DmStoreDownloader;
import uk.gov.hmcts.reform.em.npa.service.RedactionService;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RedactionRequest;
import uk.gov.hmcts.reform.em.npa.service.exception.DocumentTaskProcessingException;
import uk.gov.hmcts.reform.em.npa.service.exception.FileTypeException;
import uk.gov.hmcts.reform.em.npa.service.exception.RedactionProcessingException;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Service
public class RedactionServiceImpl implements RedactionService {

    private final Logger log = LoggerFactory.getLogger(RedactionServiceImpl.class);

    private DmStoreDownloader dmStoreDownloader;
    private PdfRedaction pdfRedaction;
    private ImageRedaction imageRedaction;
    private MarkUpRepository markUpRepository;
    private SecurityUtils securityUtils;
    private CdamService cdamService;

    @Value("#{'${redaction.multipart.image-ext}'.split(',')}")
    List<String> imageExtensionsList;

    @Value("${toggles.cdam_enabled}")
    boolean cdamEnabled;

    public RedactionServiceImpl(
            DmStoreDownloader dmStoreDownloader,
            PdfRedaction pdfRedaction,
            ImageRedaction imageRedaction,
            MarkUpRepository markUpRepository,
            SecurityUtils securityUtils,
            CdamService cdamService
    ) {
        this.dmStoreDownloader = dmStoreDownloader;
        this.pdfRedaction = pdfRedaction;
        this.imageRedaction = imageRedaction;
        this.markUpRepository = markUpRepository;
        this.securityUtils = securityUtils;
        this.cdamService = cdamService;
    }

    @Override
    public File redactFile(String auth, String serviceAuth, RedactionRequest redactionRequest) {
        try {
            File originalFile;
            log.info("cdamEnabled is : {} for documentId : {} ", cdamEnabled,
                    redactionRequest.getDocumentId());
            if (cdamEnabled) {
                originalFile = cdamService.downloadFile(auth, serviceAuth, redactionRequest.getDocumentId());
            } else {
                originalFile = dmStoreDownloader.downloadFile(redactionRequest.getDocumentId().toString());
            }

            String fileType = FilenameUtils.getExtension(originalFile.getName());

            File updatedFile;
            if (fileType.equalsIgnoreCase("pdf")) {
                log.debug("Applying redaction to PDF file");
                updatedFile = pdfRedaction.redactPdf(originalFile, redactionRequest.getRedactions());
            } else if (imageExtensionsList.contains(fileType.toLowerCase())) {
                log.debug("Applying redaction to Image Document");
                updatedFile = imageRedaction.redactImage(
                        originalFile,
                        redactionRequest.getRedactions().get(0).getRectangles()
                );
            } else {
                throw new FileTypeException("Redaction cannot be applied to the file type provided");
            }
//            markUpRepository.deleteAllByDocumentIdAndCreatedBy(redactionRequest.getDocumentId(),
//                securityUtils.getCurrentUserLogin().orElse(Constants.ANONYMOUS_USER));
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