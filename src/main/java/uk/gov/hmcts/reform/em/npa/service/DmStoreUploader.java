package uk.gov.hmcts.reform.em.npa.service;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.reform.em.npa.domain.DocumentTask;
import uk.gov.hmcts.reform.em.npa.service.impl.DocumentTaskProcessingException;

import java.io.File;

public interface DmStoreUploader {

    void uploadFile(File file, DocumentTask documentTask) throws DocumentTaskProcessingException;

    JsonNode uploadNewDocument(File file) throws DocumentTaskProcessingException;

}
