package uk.gov.hmcts.reform.em.npa.service;

import uk.gov.hmcts.reform.em.npa.domain.DocumentTask;
import uk.gov.hmcts.reform.em.npa.service.impl.DocumentTaskProcessingException;

import javax.print.Doc;
import java.io.File;

public interface DmStoreUploader {

    void uploadFile(File file, DocumentTask documentTask) throws DocumentTaskProcessingException;;

}
