package uk.gov.hmcts.reform.em.npa.service;

import uk.gov.hmcts.reform.em.npa.domain.DocumentTask;
import uk.gov.hmcts.reform.em.npa.service.impl.DocumentTaskProcessingException;

import java.io.File;

public interface DmStoreUploader {

    void uploadFile(File file, DocumentTask documentTask) throws DocumentTaskProcessingException;

    void uploadNewDocumentVersion(File file, String documentId) throws DocumentTaskProcessingException;

}
