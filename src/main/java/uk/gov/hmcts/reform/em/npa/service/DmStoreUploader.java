package uk.gov.hmcts.reform.em.npa.service;

import uk.gov.hmcts.reform.em.npa.service.impl.DocumentTaskProcessingException;

import java.io.File;

public interface DmStoreUploader {

    String uploadFile(File file) throws DocumentTaskProcessingException;;

}
