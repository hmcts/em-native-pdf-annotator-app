package uk.gov.hmcts.reform.em.npa.redaction;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.npa.service.dto.external.redaction.RedactionDTO;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

@Service
public class PdfRedaction {

    @Autowired
    private ImageRedaction imageRedaction;

    public File redaction(File documentFile, List<RedactionDTO> redactionDTOList) throws IOException {
        PDDocument document = PDDocument.load(documentFile);

        document.setDocumentInformation(new PDDocumentInformation());

        PDFRenderer pdfRenderer = new PDFRenderer(document);

        // iterate through redaction list and redact required pages and areas
        for (RedactionDTO redactionDTO : redactionDTOList) {
            // how to handle pages that have multiple redaction?
            File pageImage = transformToImage(pdfRenderer, redactionDTO.getPageNumber() /* -1 ? */);
            PDPage newPage = transformToPdf(pageImage, redactionDTO);
            document = replacePage(document, redactionDTO.getPageNumber(), newPage);
        }

        final File newFile = File.createTempFile("altered", ".pdf");
        document.save(newFile);
        document.close();

        return newFile;
    }

    /**
     * need to remove old page and replace with new PDPage
     *
     * @param document
     * @param index
     * @param page
     * @return
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

    /**
     * Transform PDF Page to Image
     *
     * @param pdfRenderer
     * @param pageNumber
     * @return
     * @throws IOException
     */
    private File transformToImage(PDFRenderer pdfRenderer, int pageNumber) throws IOException {
        // page number is 0 indexed so may need to subtract 1 from value in DTO
        BufferedImage img = pdfRenderer.renderImageWithDPI(pageNumber, 300, ImageType.RGB);

        final File alteredImage = File.createTempFile("altered", ".png");
        ImageIO.write(img, "png", alteredImage);

        return alteredImage;
    }

    /**
     * convert it back to pdf after redaction on image
     *
     * @param pageImage
     * @param redactionDTO
     * @return
     * @throws IOException
     */
    private PDPage transformToPdf(File pageImage, RedactionDTO redactionDTO) throws IOException {
        PDDocument newDocument = new PDDocument();
        PDPage newPage = new PDPage();
        PDRectangle mediaBox = newPage.getMediaBox();
        newDocument.addPage(newPage);

        BufferedImage awtImage = ImageIO.read(imageRedaction.redaction(pageImage, redactionDTO));
        PDImageXObject pdImageXObject = LosslessFactory.createFromImage(newDocument, awtImage);
        PDPageContentStream contentStream = new PDPageContentStream(newDocument, newPage, PDPageContentStream.AppendMode.APPEND, false);

        contentStream.drawImage(pdImageXObject, 0, 0, mediaBox.getWidth(), mediaBox.getHeight());
        contentStream.close();

        return newPage;
    }
}
