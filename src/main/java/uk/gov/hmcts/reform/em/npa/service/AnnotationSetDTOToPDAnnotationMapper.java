package uk.gov.hmcts.reform.em.npa.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import uk.gov.hmcts.reform.em.npa.service.dto.external.annotation.AnnotationDTO;
import uk.gov.hmcts.reform.em.npa.service.exception.DocumentTaskProcessingException;

import java.util.Set;

public interface AnnotationSetDTOToPDAnnotationMapper {

    void toNativeAnnotationsPerPage(PDDocument document, Set<AnnotationDTO> annotations) throws DocumentTaskProcessingException;


}
