package uk.gov.hmcts.reform.em.npa.service;

import uk.gov.hmcts.reform.em.npa.service.exception.DocumentTaskProcessingException;

import java.io.File;

public interface DmStoreDownloader {

    File downloadFile(String id) throws DocumentTaskProcessingException;

}
