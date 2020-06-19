package uk.gov.hmcts.reform.em.npa.service;

import uk.gov.hmcts.reform.em.npa.service.dto.external.annotation.AnnotationSetDTO;
import uk.gov.hmcts.reform.em.npa.service.exception.DocumentTaskProcessingException;

import java.io.File;

public interface PdfAnnotator {

    File annotatePdf(File file, AnnotationSetDTO annotationSetDTO) throws DocumentTaskProcessingException;

}
