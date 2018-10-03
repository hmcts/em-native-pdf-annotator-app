package uk.gov.hmcts.reform.em.npa.service.impl;

import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationPopup;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationTextMarkup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.em.npa.service.AnnotationSetDTOToPDAnnotationMapper;
import uk.gov.hmcts.reform.em.npa.service.dto.external.annotation.AnnotationDTO;
import uk.gov.hmcts.reform.em.npa.service.dto.external.annotation.CommentDTO;
import uk.gov.hmcts.reform.em.npa.service.dto.external.annotation.RectangleDTO;

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

            String allComments = annotationDTO.getComments() != null ?
                    annotationDTO.getComments().stream().sorted(Comparator.comparing(CommentDTO::getCreatedDate)).map(CommentDTO::getContent).collect(Collectors.joining("\n")) : null;

            if (annotationDTO.getRectangles() != null) {

                annotationDTO.getRectangles().forEach(rectangleDTO -> {

                    PDRectangle position = rectangleDTOToPDRectangle(rectangleDTO);

                    PDAnnotationTextMarkup markup = new PDAnnotationTextMarkup(PDAnnotationTextMarkup.SUB_TYPE_HIGHLIGHT);
                    markup.setContents(allComments);
                    markup.setColor(getColor(annotationDTO));
                    markup.setRectangle(position);ma
                    markup.setQuadPoints(getQuadsWithRectangle(position));
                    currentPageList.add(markup);

                    PDAnnotationPopup pdAnnotationPopup = new PDAnnotationPopup();
                    pdAnnotationPopup.setContents(allComments);
                    pdAnnotationPopup.setRectangle(position);
                    currentPageList.add(pdAnnotationPopup);

                });
            }

        });

        return result;
    }

    private PDColor getColor(AnnotationDTO annotationDTO) {
        if (annotationDTO.getColor() != null && annotationDTO.getColor().length() == 6) {
            int c = Integer.parseInt(annotationDTO.getColor(), 16);
            float r = ((c & 0xFF0000) >> 16) / 255f;
            float g = ((c & 0x00FF00) >> 8) / 255f;
            float b = ((c & 0x0000FF) >> 0) / 255f;
            return new PDColor( new float[] { r, g, b }, PDDeviceRGB.INSTANCE);
        } else {
            return new PDColor(new float[]{1f, 1f, 0}, PDDeviceRGB.INSTANCE);
        }
    }

    private PDRectangle rectangleDTOToPDRectangle(RectangleDTO rectangleDTO) {
        PDRectangle pdRectangle = new PDRectangle();
        pdRectangle.setLowerLeftX(rectangleDTO.getX().floatValue());
        pdRectangle.setLowerLeftY(rectangleDTO.getY().floatValue());
        pdRectangle.setUpperRightX((rectangleDTO.getX().floatValue() + rectangleDTO.getWidth().floatValue()));
        pdRectangle.setUpperRightY(rectangleDTO.getY().floatValue() + rectangleDTO.getHeight().floatValue());
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
