package uk.gov.hmcts.reform.em.npa.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.em.npa.service.PdfAnnotator;
import uk.gov.hmcts.reform.em.npa.service.dto.external.annotation.AnnotationSetDTO;

import java.io.File;

@Service
@Transactional
public class PdfAnnotatorImpl implements PdfAnnotator {
    @Override
    public File annotatePdf(File file, AnnotationSetDTO annotationSetDTO) {
        //throw new DocumentTaskProcessingException("x", null);
        return file;
    }
}
