package uk.gov.hmcts.reform.em.npa.service;

import java.util.UUID;

public interface DeleteService {
    void deleteByDocumentId(UUID documentId);
}
