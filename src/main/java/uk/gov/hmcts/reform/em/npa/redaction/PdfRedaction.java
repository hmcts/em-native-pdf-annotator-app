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
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RectangleDTO;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RedactionDTO;
import uk.gov.hmcts.reform.em.npa.service.exception.RedactionProcessingException;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

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

        try (PdfDocument pdfDocument = new PdfDocument(new PdfReader(documentFile), new PdfWriter(newFile))) {
            PdfCleanUpTool cleaner = new PdfCleanUpTool(pdfDocument, cleanUpLocations, new CleanUpProperties());
            cleaner.cleanUp();
        } catch (CleanUpImageUtil.CleanupImageHandlingUtilException e) {
            log.info("Saving redactions failed with error: {}", e.getMessage());
            throw e;
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

    /**
     * Transform PDF Page to Image.
     *
     * @param pdfRenderer pdf file to be redacted
     * @param pageNumber The page number in the PDF (zero indexed)
     * @return The file containing the converted page
     * @throws IOException if document process and image process fails
     */
    private File transformToImage(PDFRenderer pdfRenderer, int pageNumber) throws IOException {
        BufferedImage img = pdfRenderer.renderImageWithDPI(pageNumber, 300, ImageType.ARGB);
        final File alteredImage = File.createTempFile("altered", ".png");
        ImageIO.write(img, "png", alteredImage);
        return alteredImage;
    }

    /**
     * Convert the page back to pdf after redaction on image.
     *
     * @param pageImage The file containing the PDF page image
     * @return The newly redacted page in PDF format
     * @throws IOException if document process fails
     */
    private PDPage transformToPdf(File pageImage, PDDocument newDocument, PDPage originalPage) throws IOException {
        PDPage newPage = new PDPage(originalPage.getMediaBox());
        newPage.setCropBox(originalPage.getCropBox());

        try (PDPageContentStream contentStream = new PDPageContentStream(newDocument, newPage,
            PDPageContentStream.AppendMode.APPEND, false)) {
            PDRectangle cropBox = newPage.getCropBox();
            newDocument.addPage(newPage);

            BufferedImage awtImage = ImageIO.read(pageImage);
            PDImageXObject pdImageXObject = LosslessFactory.createFromImage(newDocument, awtImage);
            contentStream.drawImage(
                    pdImageXObject,
                    cropBox.getLowerLeftX(),
                    cropBox.getLowerLeftY(),
                    cropBox.getWidth(),
                    cropBox.getHeight()
            );
            return newPage;
        } catch (IOException e) {
            throw new RedactionProcessingException("Error transforming image file to PDF page");
        }
    }

    /**
     * Replace old version of page in PDF with newly redacted one.
     *
     * @param document The PDF document to be redacted
     * @param index The page number in the PDF (zero indexed)
     * @param page The redacted page to be inserted into the PDF
     * @return the pdf document with the newly redacted page inserted at the required position
     */
    private PDDocument replacePage(PDDocument document, final int index, final PDPage page) {
        if (index >= document.getNumberOfPages()) {
            document.addPage(page);
        } else {
            PDPageTree allPages = document.getDocumentCatalog().getPages();
            PDPage pageToRemove = allPages.get(index);
            allPages.insertBefore(page, pageToRemove);
            document.removePage(pageToRemove);
        }
        return document;
    }
}
