package uk.gov.hmcts.reform.em.npa.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.npa.ccd.client.CcdDataApiCaseUpdater;
import uk.gov.hmcts.reform.em.npa.ccd.client.CcdDataApiEventCreator;
import uk.gov.hmcts.reform.em.npa.ccd.domain.CcdCaseDocument;
import uk.gov.hmcts.reform.em.npa.ccd.domain.CcdDocument;
import uk.gov.hmcts.reform.em.npa.ccd.dto.CcdCallbackDto;
import uk.gov.hmcts.reform.em.npa.domain.MarkUpDTO;
import uk.gov.hmcts.reform.em.npa.redaction.ImageRedaction;
import uk.gov.hmcts.reform.em.npa.redaction.PdfRedaction;
import uk.gov.hmcts.reform.em.npa.service.DmStoreDownloader;
import uk.gov.hmcts.reform.em.npa.service.DmStoreUploader;
import uk.gov.hmcts.reform.em.npa.service.RedactionService;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class RedactionServiceImpl implements RedactionService {

    private final Logger log = LoggerFactory.getLogger(RedactionServiceImpl.class);

    private CcdDataApiEventCreator ccdDataApiEventCreator;
    private CcdDataApiCaseUpdater ccdDataApiCaseUpdater;
    private DmStoreDownloader dmStoreDownloader;
    private DmStoreUploader dmStoreUploader;
    private PdfRedaction pdfRedaction;
    private ImageRedaction imageRedaction;

    @Value("${ccd.event.trigger}")
    String ccdEventTrigger;

    @Value("#{'${redaction.multipart.image-ext}'.split(',')}")
    List<String> imageExtensionsList;

    public RedactionServiceImpl (CcdDataApiEventCreator ccdDataApiEventCreator, CcdDataApiCaseUpdater ccdDataApiCaseUpdater,
                                 DmStoreDownloader dmStoreDownloader, DmStoreUploader dmStoreUploader,
                                 PdfRedaction pdfRedaction, ImageRedaction imageRedaction) {
        this.ccdDataApiEventCreator = ccdDataApiEventCreator;
        this.ccdDataApiCaseUpdater = ccdDataApiCaseUpdater;
        this.dmStoreDownloader = dmStoreDownloader;
        this.dmStoreUploader = dmStoreUploader;
        this.pdfRedaction = pdfRedaction;
        this.imageRedaction = imageRedaction;
    }

    @Override
    public String redactFile(String jwt, String caseId, UUID documentId, List<MarkUpDTO> markUpDTOList) {
        CcdCallbackDto ccdCallbackDto = null;

        try {
            ccdCallbackDto = ccdDataApiEventCreator.executeTrigger(caseId, ccdEventTrigger, jwt);

            File originalFile = dmStoreDownloader.downloadFile(documentId.toString());
            String fileType = FilenameUtils.getExtension(originalFile.getName());

            File updatedFile;
            if (fileType.equals("pdf")) {
                log.info("Applying redaction to PDF file");
                updatedFile = pdfRedaction.redaction(originalFile, markUpDTOList);
            } else if (imageExtensionsList.contains(fileType)) {
                log.info("Applying redaction to Image Document");
                updatedFile = imageRedaction.redaction(originalFile, markUpDTOList);
            } else {
                throw new FileTypeException("Redaction cannot be applied to the file type provided");
            }

            JsonNode updatedDocRes = dmStoreUploader.uploadDocument(updatedFile);

            updateCcdCaseDocuments(ccdCallbackDto, updatedDocRes, originalFile);
            // delete markups for session
        } catch (DocumentTaskProcessingException e) {
            log.error(e.getMessage(), e);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new FileTypeException("File processing error");
        } finally {
            if (ccdCallbackDto != null) {
                ccdDataApiCaseUpdater.executeUpdate(ccdCallbackDto, jwt);
            }
        }
        return documentId.toString();
    }

    private void updateCcdCaseDocuments(CcdCallbackDto ccdCallbackDto,
                                        JsonNode documentStoreResponse,
                                        File originalDocumentFile) throws JsonProcessingException {

        ObjectMapper mapper = new ObjectMapper();
        JsonNode caseDocuments = ccdCallbackDto.getCaseData().findValue("caseDocuments");

        CcdCaseDocument caseDocument =
                CcdCaseDocument.builder()
                        .documentName(originalDocumentFile.getName() + "-Redacted")
                        .documentType("") // need to find a way to get this doc type
                        .documentLink(
                                CcdDocument.builder()
                                        .documentUrl(documentStoreResponse.at("/_embedded/documents").get(0).at("/_links/self/href").asText())
                                        .documentFileName(documentStoreResponse.at("/_embedded/documents").get(0).at("/originalDocumentName").asText())
                                        .documentBinaryUrl(documentStoreResponse.at("/_embedded/documents").get(0).at("/_links/binary/href").asText())
                                        .build()
                        )
                        .createdDatetime(LocalDateTime.now())
                        .size(documentStoreResponse.at("/_embedded/documents").get(0).at("/size").asLong())
                        .createdBy(documentStoreResponse.at("/_embedded/documents").get(0).at("/createdBy").asText())
                        .build();

        ((ArrayNode) caseDocuments).add(mapper.writeValueAsString(caseDocument));
    }
}