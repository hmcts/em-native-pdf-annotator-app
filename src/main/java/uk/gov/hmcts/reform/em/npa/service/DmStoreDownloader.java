package uk.gov.hmcts.reform.em.npa.service;

import java.io.File;

public interface DmStoreDownloader {

    File downloadFile(String id);

}
