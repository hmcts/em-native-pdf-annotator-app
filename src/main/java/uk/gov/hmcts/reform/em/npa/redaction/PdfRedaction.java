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
            RectangleDTO rectangle) {
        PDPage page = document.getPage(pageNumber);
        PDRectangle pageSize = page.getMediaBox();
        pageSize.setLowerLeftX(page.getCropBox().getLowerLeftX() / 0.75f);
        pageSize.setLowerLeftY(page.getCropBox().getLowerLeftY() / 0.75f);
        pageSize.setUpperRightX(page.getCropBox().getUpperRightX() / 0.75f);
        pageSize.setUpperRightY(page.getCropBox().getUpperRightY());

        return new Rectangle(
            pixelToPointConversion(pageSize.getLowerLeftX() + rectangle.getX()),
            (pageSize.getHeight() - pixelToPointConversion(rectangle.getY()))
                - pixelToPointConversion(rectangle.getHeight()),
            pixelToPointConversion(rectangle.getWidth()),
            pixelToPointConversion(rectangle.getHeight()));

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
