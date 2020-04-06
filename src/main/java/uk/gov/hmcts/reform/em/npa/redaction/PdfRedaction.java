package uk.gov.hmcts.reform.em.npa.redaction;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
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

        // remove all metadata from document
        document.setDocumentInformation(new PDDocumentInformation());

        PDFRenderer pdfRenderer = new PDFRenderer(document);

        // iterate through redaction list and redact required pages and areas
        for (RedactionDTO redactionDTO : redactionDTOList) {
            // how to handle pages that have multiple redaction?
            File pageImage = transformToImage(pdfRenderer, redactionDTO.getPageNumber() /* -1 ? */);

            // convert it back to pdf after redaction on image
            PDPage newPage = transformToPdf(pageImage, redactionDTO);

            // need to remove old page and replace with new PDPage
            document.removePage(redactionDTO.getPageNumber());
        }
        document.close();

        // save pdf document file
        final File newFile = File.createTempFile("altered", ".pdf");
        document.save(newFile);
        document.close();

        return newFile;
    }

    private void insertPageIntoPdf() {

    }

    private File transformToImage(PDFRenderer pdfRenderer, int pageNumber) throws IOException {
        // page number is 0 indexed so may need to subtract 1 from value in DTO
        BufferedImage img = pdfRenderer.renderImageWithDPI(pageNumber, 300, ImageType.RGB);

        final File alteredImage = File.createTempFile("altered", ".png");
        ImageIOUtil.writeImage(img, alteredImage.getName(), 300);
//        ImageIO.write(bim, "png", alteredImage);
        return alteredImage;
    }

    private PDPage transformToPdf(File pageImage, RedactionDTO redactionDTO) throws IOException {
        PDDocument newDocument = new PDDocument();
        PDPage newPage = new PDPage();
        newDocument.addPage(newPage);

        BufferedImage awtImage = ImageIO.read(imageRedaction.redaction(pageImage, redactionDTO));
        PDImageXObject pdImageXObject = LosslessFactory.createFromImage(newDocument, awtImage);
        PDPageContentStream contentStream = new PDPageContentStream(newDocument, newPage, PDPageContentStream.AppendMode.APPEND, false);
        contentStream.drawImage(pdImageXObject, 0, 0, awtImage.getWidth(), awtImage.getHeight());
        contentStream.close();

        return newPage;
    }
}
