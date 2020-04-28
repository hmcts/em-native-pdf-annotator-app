package uk.gov.hmcts.reform.em.npa.service;

import uk.gov.hmcts.reform.em.npa.service.dto.external.annotation.AnnotationSetDTO;
import uk.gov.hmcts.reform.em.npa.service.exception.DocumentTaskProcessingException;

public interface AnnotationSetFetcher {

    AnnotationSetDTO fetchAnnotationSet(String documentId, String jwt) throws DocumentTaskProcessingException;;

}
