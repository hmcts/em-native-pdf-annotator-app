package uk.gov.hmcts.reform.em.npa.service.impl;

import io.jsonwebtoken.lang.Assert;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.junit.Test;
import uk.gov.hmcts.reform.em.npa.service.dto.external.annotation.AnnotationDTO;
import uk.gov.hmcts.reform.em.npa.service.dto.external.annotation.CommentDTO;
import uk.gov.hmcts.reform.em.npa.service.dto.external.annotation.RectangleDTO;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AnnotationSetDTOToPDAnnotationMapperImplTest {

    AnnotationSetDTOToPDAnnotationMapperImpl annotationSetDTOToPDAnnotationMapperImpl =
        new AnnotationSetDTOToPDAnnotationMapperImpl();

    @Test
    public void testSuccessfulRun() throws Exception {
        Set<AnnotationDTO> annotationSet = new HashSet<>();

        AnnotationDTO annotationDTO = new AnnotationDTO();
        annotationDTO.setPage(0);
        annotationDTO.setAnnotationType("height");

        Set<RectangleDTO> rectangles = new HashSet<>();

        RectangleDTO rectangleDTO = new RectangleDTO();
        rectangleDTO.setX(0f);
        rectangleDTO.setY(0f);
        rectangleDTO.setHeight(10f);
        rectangleDTO.setWidth(10f);
        rectangles.add(rectangleDTO);

        Set<CommentDTO> commentDTOS = new HashSet<>();
        CommentDTO commentDTO = new CommentDTO();
        commentDTO.setContent("comment");

        annotationDTO.setRectangles(rectangles);
        annotationDTO.setComments(commentDTOS);

        annotationSet.add(annotationDTO);
        Map<Integer, List<PDAnnotation>> result = annotationSetDTOToPDAnnotationMapperImpl.toNativeAnnotationsPerPage(annotationSet);

        Assert.notNull(result.get(0).get(0));


    }

}
