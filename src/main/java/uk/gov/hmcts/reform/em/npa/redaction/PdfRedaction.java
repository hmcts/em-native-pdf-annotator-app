package uk.gov.hmcts.reform.em.npa.redaction;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RectangleDTO;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RedactionDTO;

import java.io.File;
import java.io.IOException;
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
    @SuppressWarnings("squid:S5443")
    public File redactPdf(File documentFile, List<RedactionDTO> redactionDTOList) throws IOException {
        final File newFile = File.createTempFile(
                String.format("Redacted-%s", FilenameUtils.getBaseName(documentFile.getName())),
                ".pdf"
        );

        try (PdfReader reader = new PdfReader(documentFile);
             PdfWriter writer = new PdfWriter(newFile);
             PdfDocument pdfDocument = new PdfDocument(reader, writer)) {

            for (RedactionDTO redactionDTO : redactionDTOList) {
                PdfPage page = pdfDocument.getPage(redactionDTO.getPage());
                PdfCanvas canvas = new PdfCanvas(page);

                for (RectangleDTO rectangleDTO : redactionDTO.getRectangles()) {
                    Rectangle rect = createRectangle(pdfDocument, redactionDTO.getPage(), rectangleDTO);

                    canvas.saveState()
                            .setFillColor(ColorConstants.BLACK)
                            .rectangle(rect)
                            .fill()
                            .restoreState();
                }
            }

        } catch (Exception e) {
            log.error("Redaction failed with error: {}", e.getMessage());
            throw new IOException("Failed to redact PDF", e);
        }

        return newFile;
    }


    private Rectangle createRectangle(PdfDocument pdfDocument, int pageNumber, RectangleDTO rectangleDTO) {
        PdfPage page = pdfDocument.getPage(pageNumber);
        Rectangle pageSize = page.getPageSize();

        float x = pixelToPointConversion(rectangleDTO.getX());
        float y = pixelToPointConversion(rectangleDTO.getY());
        float width = pixelToPointConversion(rectangleDTO.getWidth());
        float height = pixelToPointConversion(rectangleDTO.getHeight());

        // Adjust y-coordinate (PDF coordinate system starts from bottom-left)
        y = pageSize.getHeight() - y - height;

        return new Rectangle(x, y, width, height);
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
