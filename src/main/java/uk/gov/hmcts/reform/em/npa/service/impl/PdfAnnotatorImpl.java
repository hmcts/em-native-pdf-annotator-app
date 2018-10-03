package uk.gov.hmcts.reform.em.npa.service.impl;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.em.npa.service.AnnotationSetDTOToPDAnnotationMapper;
import uk.gov.hmcts.reform.em.npa.service.PdfAnnotator;
import uk.gov.hmcts.reform.em.npa.service.dto.external.annotation.AnnotationSetDTO;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class PdfAnnotatorImpl implements PdfAnnotator {

    private final Logger log = LoggerFactory.getLogger(PdfAnnotatorImpl.class);

    private final AnnotationSetDTOToPDAnnotationMapper annotationSetDTOToPDAnnotationMapper;

    public PdfAnnotatorImpl(AnnotationSetDTOToPDAnnotationMapper annotationSetDTOToPDAnnotationMapper) {
        this.annotationSetDTOToPDAnnotationMapper = annotationSetDTOToPDAnnotationMapper;
    }

    @Override
    public File annotatePdf(File file, AnnotationSetDTO annotationSetDTO) throws DocumentTaskProcessingException {

        try (PDDocument document = PDDocument.load(file)) {

            Map<Integer, List<PDAnnotation>> nativeAnnotationsPerPage = annotationSetDTOToPDAnnotationMapper.toNativeAnnotationsPerPage(annotationSetDTO.getAnnotations());

            nativeAnnotationsPerPage.entrySet().stream().forEach( entry -> {
                PDPage page = document.getPage(entry.getKey());
                try {
                    page.getAnnotations().addAll(entry.getValue());
                } catch (IOException e) {
                    System.out.print(e.getMessage()); e.printStackTrace();
                    log.error("Error processing annotation set", e);
                }
            });

            final String annotatedPathFile = file.getAbsolutePath().replaceFirst("\\.pdf", "-annotated.pdf");
            document.save(annotatedPathFile);
            return new File(annotatedPathFile);
        } catch (IOException e) {
            throw new DocumentTaskProcessingException("Could not load the file " + file.getName(), e);
        }


    }
}
