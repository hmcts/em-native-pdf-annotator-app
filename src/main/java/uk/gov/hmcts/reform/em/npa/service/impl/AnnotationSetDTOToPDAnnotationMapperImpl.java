package uk.gov.hmcts.reform.em.npa.service.impl;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationPopup;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationTextMarkup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.em.npa.Application;
import uk.gov.hmcts.reform.em.npa.batch.DocumentTaskItemProcessor;
import uk.gov.hmcts.reform.em.npa.service.AnnotationSetDTOToPDAnnotationMapper;
import uk.gov.hmcts.reform.em.npa.service.dto.external.annotation.AnnotationDTO;
import uk.gov.hmcts.reform.em.npa.service.dto.external.annotation.CommentDTO;
import uk.gov.hmcts.reform.em.npa.service.dto.external.annotation.RectangleDTO;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AnnotationSetDTOToPDAnnotationMapperImpl implements AnnotationSetDTOToPDAnnotationMapper {

    private final Logger log = LoggerFactory.getLogger(AnnotationSetDTOToPDAnnotationMapperImpl.class);

    @Override
    public Map<Integer, List<PDAnnotation>> toNativeAnnotationsPerPage(Set<AnnotationDTO> annotations) throws DocumentTaskProcessingException {


        Map<Integer, List<PDAnnotation>> result = new HashMap<>();

        annotations.stream().forEach( annotationDTO -> {

            Integer pageNumber = annotationDTO.getPage();

            if (!result.containsKey(pageNumber)) {
                result.put(pageNumber, new LinkedList<>());
            }

            List<PDAnnotation> currentPageList = result.get(pageNumber);

            String allComments = annotationDTO.getComments().stream()
                .sorted(Comparator.comparing(CommentDTO::getCreatedDate)).map(CommentDTO::getContent).collect(Collectors.joining("\n"));

            annotationDTO.getRectangles().forEach(rectangleDTO -> {

                getAnnotationsTemplate().forEach( a -> {
                    a.setPage(null);
                    PDRectangle position = rectangleDTOToPDRectangle(rectangleDTO);
                    if (a instanceof PDAnnotationTextMarkup) {
                        PDAnnotationTextMarkup newOne = new PDAnnotationTextMarkup(a.getCOSObject());
                        newOne.setContents(allComments);
                        newOne.setRectangle(position);
                        newOne.setQuadPoints(getQuadsWithRectangle(position));
                        currentPageList.add(a);
                    } else if (a instanceof PDAnnotationPopup) {
                        PDAnnotationPopup newOne = new PDAnnotationPopup(a.getCOSObject());
                        newOne.setContents(allComments);
                        newOne.setRectangle(position);
                        currentPageList.add(a);
                    }

                });

            });

        });

        return result;
    }

    private List<PDAnnotation> getAnnotationsTemplate()  {

        try {
            ClassLoader classLoader = Application.class.getClassLoader();
            File file = new File(classLoader.getResource("annotationTemplate.pdf").getFile());
            PDDocument templateDocument = PDDocument.load(file);
            return templateDocument.getPage(0).getAnnotations();
        } catch (IOException e) {
            log.error("Could not retrieve the template", e);
            return Collections.EMPTY_LIST;
        }

    }

    private PDRectangle rectangleDTOToPDRectangle(RectangleDTO rectangleDTO) {
        PDRectangle pdRectangle = new PDRectangle();
        pdRectangle.setLowerLeftX(rectangleDTO.getX());
        pdRectangle.setLowerLeftY(rectangleDTO.getY());
        pdRectangle.setUpperRightX(rectangleDTO.getX() + rectangleDTO.getWidth());
        pdRectangle.setUpperRightY(rectangleDTO.getY() + rectangleDTO.getHeight());
        return pdRectangle;
    }

    public static float[] getQuadsWithRectangle(PDRectangle position) {

        // work out the points forming the four corners of the annotations
        // set out in anti clockwise form (Completely wraps the text)
        // OK, the below doesn't match that description.
        // It's what acrobat 7 does and displays properly!
        float[] quads = new float[8];

        quads[0] = position.getLowerLeftX();  // x1
        quads[1] = position.getUpperRightY() - 2; // y1
        quads[2] = position.getUpperRightX(); // x2
        quads[3] = quads[1]; // y2
        quads[4] = quads[0];  // x3
        quads[5] = position.getLowerLeftY() - 2; // y3
        quads[6] = quads[2]; // x4
        quads[7] = quads[5]; // y5

        return quads;
    }
}
