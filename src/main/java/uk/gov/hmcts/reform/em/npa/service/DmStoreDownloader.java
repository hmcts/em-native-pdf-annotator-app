package uk.gov.hmcts.reform.em.npa.service;

import uk.gov.hmcts.reform.em.npa.service.exception.DocumentTaskProcessingException;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public interface DmStoreDownloader {

    File downloadFile(String id) throws DocumentTaskProcessingException;

    File downloadFile(String auth, String serviceAuth, UUID documentId) throws DocumentTaskProcessingException, IOException;

}
