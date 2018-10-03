package uk.gov.hmcts.reform.em.npa.service.impl;

import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.junit.Assert;
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
        annotationDTO.setAnnotationType("highlight");

        Set<RectangleDTO> rectangles = new HashSet<>();

        RectangleDTO rectangleDTO = new RectangleDTO();
        rectangleDTO.setX(0d);
        rectangleDTO.setY(0d);
        rectangleDTO.setHeight(10d);
        rectangleDTO.setWidth(10d);
        rectangles.add(rectangleDTO);

        Set<CommentDTO> commentDTOS = new HashSet<>();
        CommentDTO commentDTO = new CommentDTO();
        commentDTO.setContent("comment");

        annotationDTO.setRectangles(rectangles);
        annotationDTO.setComments(commentDTOS);

        annotationSet.add(annotationDTO);
        Map<Integer, List<PDAnnotation>> result = annotationSetDTOToPDAnnotationMapperImpl.toNativeAnnotationsPerPage(annotationSet);

        Assert.assertNotNull(result.get(0).get(0));
        Assert.assertEquals(result.get(0).get(0).getColor().getComponents()[0], 1f, 0);
        Assert.assertEquals(result.get(0).get(0).getColor().getComponents()[1], 1f, 0);
        Assert.assertEquals(result.get(0).get(0).getColor().getComponents()[2], 0f, 0);


    }

}
