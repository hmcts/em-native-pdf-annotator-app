package uk.gov.hmcts.reform.em.npa.service;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.reform.em.npa.domain.DocumentTask;
import uk.gov.hmcts.reform.em.npa.service.exception.DocumentTaskProcessingException;

import java.io.File;

public interface DmStoreUploader {

    void uploadFile(File file, DocumentTask documentTask) throws DocumentTaskProcessingException;

    JsonNode uploadDocument(File file) throws DocumentTaskProcessingException;

}
