package uk.gov.hmcts.reform.em.npa.redaction;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.contentstream.PDFStreamEngine;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.util.Matrix;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ImageDpiExtractor extends PDFStreamEngine {

    private final List<ImageDpiInfo> dpiInfos = new ArrayList<>();

    @Override
    protected void processOperator(Operator operator, List<COSBase> operands) throws IOException {

        // This code attempts to get the displayed size from the image's transformation matrix if available;
        // otherwise, it defaults to the image's pixel size.
        //The DPI is then calculated based on the ratio of pixel size to displayed size in points.

        if ("Do".equals(operator.getName())) {
            COSName objectName = (COSName) operands.getFirst();
            PDXObject xobject = getResources().getXObject(objectName);
            if (xobject instanceof PDImageXObject image) {

                Matrix ctm = getGraphicsState().getCurrentTransformationMatrix();

                int imgWidthPx = image.getWidth();
                int imgHeightPx = image.getHeight();

                // Transform (0,0) and (imageWidth, imageHeight) to page space
                Point2D.Float origin = transformPoint(ctm, 0, 0);
                Point2D.Float xedge  = transformPoint(ctm, imgWidthPx, 0);
                Point2D.Float yedge  = transformPoint(ctm, 0, imgHeightPx);

                // Get displayed width and height in PDF user units (points)
                float displayedWidthPts = (float) origin.distance(xedge);
                float displayedHeightPts = (float) origin.distance(yedge);

                float widthInches = displayedWidthPts / 72f;
                float heightInches = displayedHeightPts / 72f;

                float dpiX = imgWidthPx / widthInches;
                float dpiY = imgHeightPx / heightInches;

                log.info("Image: {} x {} px", imgWidthPx, imgHeightPx);
                log.info("Displayed size: {} x {} pts ({} x {} in)",
                        displayedWidthPts, displayedHeightPts, widthInches, heightInches);
                log.info("DPI: {} x {}", dpiX, dpiY);

            }
        } else {
            super.processOperator(operator, operands);
        }
    }

    public List<ImageDpiInfo> extractDpi(PDPage page) throws IOException {
        dpiInfos.clear();
        processPage(page);
        return new ArrayList<>(dpiInfos);
    }

    // Helper function
    private Point2D.Float transformPoint(Matrix m, float x, float y) {
        float transformedX = m.getTranslateX() + x * m.getScaleX() + y * m.getShearX();
        float transformedY = m.getTranslateY() + x * m.getShearY() + y * m.getScaleY();
        return new Point2D.Float(transformedX, transformedY);
    }
}
