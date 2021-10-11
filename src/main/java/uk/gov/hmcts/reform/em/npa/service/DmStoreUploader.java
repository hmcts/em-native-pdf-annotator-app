package uk.gov.hmcts.reform.em.npa.service;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.reform.em.npa.service.exception.DocumentTaskProcessingException;

import java.io.File;

public interface DmStoreUploader {

    JsonNode uploadDocument(File file) throws DocumentTaskProcessingException;

}
