package uk.gov.hmcts.reform.em.npa.redaction;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.pdfcleanup.CleanUpProperties;
import com.itextpdf.pdfcleanup.PdfCleanUpLocation;
import com.itextpdf.pdfcleanup.PdfCleanUpTool;
import com.itextpdf.pdfcleanup.util.CleanUpImageUtil;
import org.apache.commons.io.FilenameUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RectangleDTO;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RedactionDTO;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfRedaction {

    private final Logger log = LoggerFactory.getLogger(PdfRedaction.class);

    /**
     * Applying Redaction to pdf file.
     *
     * @param documentFile     pdf file to be redacted
     * @param redactionDTOList list of redactions to be applied to the pdf
     * @return the redacted file
     * @throws IOException in document process
     */
    public File redactPdf(File documentFile, List<RedactionDTO> redactionDTOList) throws IOException {
        PDDocument pdDocument = Loader.loadPDF(documentFile);
        final File newFile =
                File.createTempFile(
                        String.format("Redacted-%s", FilenameUtils.getBaseName(documentFile.getName())),
                        ".pdf"
                );

        List<PdfCleanUpLocation> cleanUpLocations = new ArrayList<>();

        redactionDTOList.forEach(redactionDTO -> redactionDTO.getRectangles().forEach(rectangleDTO ->
                cleanUpLocations.add(
                        new PdfCleanUpLocation(redactionDTO.getPage(),
                                createRectangle(pdDocument, redactionDTO.getPage() - 1, rectangleDTO),
                                ColorConstants.BLACK))));

        PdfReader reader = new PdfReader(documentFile);
        reader.setUnethicalReading(true);
        try (PdfDocument pdfDocument = new PdfDocument(reader, new PdfWriter(newFile))) {
            PdfCleanUpTool cleaner = new PdfCleanUpTool(pdfDocument, cleanUpLocations, new CleanUpProperties());
            cleaner.cleanUp();
        } catch (CleanUpImageUtil.CleanupImageHandlingUtilException e) {
            log.error("Saving redactions failed with error: {}", e.getMessage());
        }
        pdDocument.close();
        return newFile;
    }

    /**
     * Draw rectangles on pdf document to redact marked up content on page.
     *
     * @param document The pdf to be redacted
     * @param pageNumber The page number in the PDF (zero indexed)
     * @param rectangle Rectangle to be drawn onto the pdf document
     * @throws IOException If it fails in document process
     */
    private Rectangle createRectangle(
            PDDocument document,
            int pageNumber,
            RectangleDTO rectangle
    ) {

        PDPage page = document.getPage(pageNumber);
        PDRectangle pageSize = page.getCropBox();

        float x = pixelToPointConversion(rectangle.getX());
        float y = pixelToPointConversion(rectangle.getY());
        float width = pixelToPointConversion(rectangle.getWidth());
        float height = pixelToPointConversion(rectangle.getHeight());

        int rotation = page.getRotation();

        float pdfX;
        float pdfY;
        switch (rotation) {
            case 90 -> {  // Bottom-right origin
                pdfX = pageSize.getLowerLeftX() + y;
                pdfY = x;
                // Swap width and height due to 90° rotation
                float temp = width;
                width = height;
                height = temp;
            }
            case 180 -> {  // Top-right origin
                pdfX = pageSize.getUpperRightX() - x;
                pdfY = pageSize.getLowerLeftY() + y;
            }
            case 270 -> {  // Top-left origin
                pdfX = pageSize.getUpperRightY() - x - width;
                pdfY = pageSize.getUpperRightX() - y - height;
                // Swap width and height due to 270° rotation
                float temp = width;
                width = height;
                height = temp;
            }
            default -> {  // 0 or 360° rotation
                pdfX = pageSize.getLowerLeftX() + x;
                pdfY = pageSize.getUpperRightY() - y - height;
            }
        }

        return new Rectangle(pdfX, pdfY, width, height);
    }

    /**
     * Convert pixel values passed by media viewer into pdf friendly point values.
     *
     * @param value Pixel value to be converted into Point value
     * @return Converted Point value
     */
    private float pixelToPointConversion(double value) {
        return (float) (0.75 * value);
    }

}
