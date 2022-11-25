package uk.gov.hmcts.reform.em.npa.redaction;

import org.apache.commons.io.FilenameUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RectangleDTO;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RedactionDTO;
import uk.gov.hmcts.reform.em.npa.service.exception.RedactionProcessingException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

@Service
public class PdfRedaction {

    /**
     * Applying Redaction to pdf file
     *
     * @param documentFile pdf file to be redacted
     * @param redactionDTOList list of redactions to be applied to the pdf
     * @return the redacted file
     * @throws IOException
     */
    public File redactPdf(File documentFile, List<RedactionDTO> redactionDTOList) throws IOException {
        PDDocument document = PDDocument.load(documentFile);
        PDFRenderer pdfRenderer = new PDFRenderer(document);
        final File newFile = File.createTempFile(String.format("Redacted-%s", FilenameUtils.getBaseName(documentFile.getName())), ".pdf");
        document.setDocumentInformation(new PDDocumentInformation());

        try (PDDocument newDocument = new PDDocument()) {
            for (RedactionDTO redactionDTO : redactionDTOList) {
                redactPageContent(document, redactionDTO.getPage() - 1, redactionDTO.getRectangles());
                File pageImage = transformToImage(pdfRenderer, redactionDTO.getPage() - 1);
                PDPage newPage = transformToPdf(pageImage, newDocument, document.getPage(redactionDTO.getPage() - 1));
                replacePage(document, redactionDTO.getPage() - 1, newPage);
            }
            document.save(newFile);
        }
        document.close();
        return newFile;
    }

    /**
     * Draw rectangles on pdf document to redact marked up content on page
     *
     * @param document The pdf to be redacted
     * @param pageNumber The page number in the PDF (zero indexed)
     * @param rectangles Rectangles to be drawn onto the pdf document
     * @throws IOException
     */
    private void redactPageContent(PDDocument document, int pageNumber, Set<RectangleDTO> rectangles) throws IOException {
        PDPage page = document.getPage(pageNumber);
        PDRectangle pageSize = page.getMediaBox();

        PDPageContentStream contentStream = new PDPageContentStream(document, page,
            PDPageContentStream.AppendMode.APPEND, true, true);
        contentStream.setNonStrokingColor(Color.BLACK);

        rectangles.stream().forEach(rectangle -> {
            try {
                contentStream.addRect(
                    pixelToPointConversion(pageSize.getLowerLeftX() + rectangle.getX()),
                    (pageSize.getHeight() - pixelToPointConversion(rectangle.getY())) -
                        pixelToPointConversion(rectangle.getHeight()),
                    pixelToPointConversion(rectangle.getWidth()),
                    pixelToPointConversion(rectangle.getHeight()));
                contentStream.fill();
            } catch (IOException e) {
                throw new RedactionProcessingException(e.getMessage());
            }
        });
        contentStream.close();
    }

    /**
     * Convert pixel values passed by media viewer into pdf friendly point values
     *
     * @param value Pixel value to be converted into Point value
     * @return Converted Point value
     */
    private float pixelToPointConversion(double value) {
        return (float) (0.75 * value);
    }

    /**
     * Transform PDF Page to Image
     *
     * @param pdfRenderer
     * @param pageNumber The page number in the PDF (zero indexed)
     * @return The file containing the converted page
     * @throws IOException
     */
    private File transformToImage(PDFRenderer pdfRenderer, int pageNumber) throws IOException {
        BufferedImage img = pdfRenderer.renderImage(pageNumber);
        final File alteredImage = File.createTempFile("altered", ".png");
        ImageIO.write(img, "png", alteredImage);
        return alteredImage;
    }

    /**
     * Convert the page back to pdf after redaction on image
     *
     * @param pageImage The file containing the PDF page image
     * @return The newly redacted page in PDF format
     * @throws IOException
     */
    private PDPage transformToPdf(File pageImage, PDDocument newDocument, PDPage originalPage) throws IOException {
        PDPage newPage = new PDPage(PDRectangle.A4);
        try (PDPageContentStream contentStream = new PDPageContentStream(newDocument, newPage,
            PDPageContentStream.AppendMode.APPEND, true)) {
            PDRectangle mediaBox = newPage.getMediaBox();
            newDocument.addPage(newPage);

            BufferedImage awtImage = ImageIO.read(pageImage);
            PDImageXObject pdImageXObject = LosslessFactory.createFromImage(newDocument, awtImage);
            contentStream.drawImage(pdImageXObject, 0, 0, mediaBox.getWidth(), mediaBox.getHeight());

            return newPage;
        } catch (IOException e) {
            throw new RedactionProcessingException("Error transforming image file to PDF page");
        }
    }

    /**
     * Replace old version of page in PDF with newly redacted one
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
