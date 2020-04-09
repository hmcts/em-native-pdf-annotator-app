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
import uk.gov.hmcts.reform.em.npa.service.impl.RedactionProcessingException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        document.setDocumentInformation(new PDDocumentInformation());

        for (Map.Entry<Integer, List<RedactionDTO>> pageRedactionSet : groupByPageNumber(redactionDTOList).entrySet()) {
            File pageImage = transformToImage(pdfRenderer, pageRedactionSet.getKey() /* -1 ? */);
            for (RedactionDTO redactionDTO : redactionDTOList) {
                pageImage = imageRedaction.redaction(pageImage, redactionDTO);
            }
            PDPage newPage = transformToPdf(pageImage);
            document = replacePage(document, pageRedactionSet.getKey() /* -1 ? */, newPage);
        }

        final File newFile = File.createTempFile("altered", ".pdf");
        document.save(newFile);
        document.close();

        return newFile;
    }

    /**
     * Group the list of redactionDTO objects by page number
     *
     * @param redactionDTOList the list to be grouped
     * @return Map consisting of page number key and Redaction list for that page
     */
    private Map<Integer, List<RedactionDTO>> groupByPageNumber(List<RedactionDTO> redactionDTOList) {
        Map<Integer, List<RedactionDTO>> resultMap = new HashMap<>();

        for (RedactionDTO redactionDTO : redactionDTOList) {
            if (!resultMap.containsKey(redactionDTO.getPageNumber())) {
                List<RedactionDTO> list = new ArrayList<>();
                list.add(redactionDTO);
                resultMap.put(redactionDTO.getPageNumber(), list);
            } else {
                resultMap.get(redactionDTO.getPageNumber()).add(redactionDTO);
            }
        }

        return resultMap;
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

    /**
     * Transform PDF Page to Image
     *
     * @param pdfRenderer
     * @param pageNumber The page number in the PDF (zero indexed)
     * @return The file containing the converted page
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
     * Convert the page back to pdf after redaction on image
     *
     * @param pageImage The file containing the PDF page image
     * @return The newly redacted page in PDF format
     * @throws IOException
     */
    private PDPage transformToPdf(File pageImage) throws IOException {
        PDDocument newDocument = new PDDocument();
        PDPageContentStream contentStream = null;

        try {
            PDPage newPage = new PDPage();
            PDRectangle mediaBox = newPage.getMediaBox();
            newDocument.addPage(newPage);

            BufferedImage awtImage = ImageIO.read(pageImage);
            PDImageXObject pdImageXObject = LosslessFactory.createFromImage(newDocument, awtImage);
            contentStream = new PDPageContentStream(newDocument, newPage, PDPageContentStream.AppendMode.APPEND, false);
            contentStream.drawImage(pdImageXObject, 0, 0, mediaBox.getWidth(), mediaBox.getHeight());

            return newPage;
        } catch (IOException e) {
            throw new RedactionProcessingException("Error transforming image file to PDF page");
        } finally {
            contentStream.close();
            newDocument.close();
        }
    }
}
