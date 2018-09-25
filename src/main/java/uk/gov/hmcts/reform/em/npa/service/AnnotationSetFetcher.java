package uk.gov.hmcts.reform.em.npa.service;

import uk.gov.hmcts.reform.em.npa.service.dto.external.annotation.AnnotationSetDTO;

public interface AnnotationSetFetcher {

    AnnotationSetDTO fetchAnnotationSet(String documentId, String jwt);

}
