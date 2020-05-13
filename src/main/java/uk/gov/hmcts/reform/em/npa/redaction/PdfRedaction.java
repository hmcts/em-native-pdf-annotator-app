package uk.gov.hmcts.reform.em.npa.redaction;

import org.apache.commons.io.FilenameUtils;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.util.Matrix;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RectangleDTO;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RedactionDTO;
import uk.gov.hmcts.reform.em.npa.service.exception.RedactionProcessingException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

@Service
public class PdfRedaction {

    @Autowired
    private ImageRedaction imageRedaction;

    /**
     * Applying Redaction to pdf file
     *
     * @param documentFile pdf file to be redacted
     * @param redactionDTOList list of redactions to be applied to the pdf
     * @return the redacted file
     * @throws IOException
     */
    public File redaction(File documentFile, List<RedactionDTO> redactionDTOList) throws IOException {
        PDDocument document = PDDocument.load(documentFile);
        PDFRenderer pdfRenderer = new PDFRenderer(document);

        final File newFile = File.createTempFile(String.format("Redacted-%s", FilenameUtils.getBaseName(documentFile.getName())), ".pdf");

        document.setDocumentInformation(new PDDocumentInformation());
        try (PDDocument newDocument = new PDDocument()) {

            for (RedactionDTO redactionDTO : redactionDTOList) {
                draw(document, redactionDTO.getPage() -1, redactionDTO.getRectangles());
                File pageImage = transformToImage(pdfRenderer, redactionDTO.getPage() - 1);
//                pageImage = imageRedaction.redaction(pageImage, redactionDTO.getRectangles());
                PDPage newPage = transformToPdf(pageImage, newDocument);
                replacePage(document, redactionDTO.getPage() - 1, newPage);
            }

            document.save(newFile);
        }

        document.close();
        return newFile;
    }

    private void draw(PDDocument document, int pageNumber, Set<RectangleDTO> rectangles) throws IOException {
        PDPage page = document.getPage(pageNumber);
        PDRectangle pageSize = page.getMediaBox();

        PDPageContentStream contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true);
        contentStream.setNonStrokingColor(Color.BLACK);

        rectangles.stream().forEach(rectangle -> {
            try {
                contentStream.addRect(
                    rectangle.getX().floatValue(),
                    (pageSize.getHeight() - Math.abs(rectangle.getY().floatValue() - rectangle.getHeight().floatValue())),
                    rectangle.getWidth().floatValue(),
                    rectangle.getHeight().floatValue());
                contentStream.fill();
            } catch (IOException e) {
                throw new RedactionProcessingException(e.getMessage());
            }
        });
        contentStream.close();
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
        BufferedImage img = pdfRenderer.renderImage(pageNumber, 1, ImageType.RGB);

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
    private PDPage transformToPdf(File pageImage, PDDocument newDocument) throws IOException {
        PDPage newPage = new PDPage();
        try (PDPageContentStream contentStream = new PDPageContentStream(newDocument, newPage, PDPageContentStream.AppendMode.APPEND, false)) {
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
