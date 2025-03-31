package uk.gov.hmcts.reform.em.npa.redaction;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.StampingProperties;
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
import java.util.Set;
import java.util.UUID;

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

        StampingProperties properties = new StampingProperties();
        properties.useAppendMode();

        try (PdfDocument pdfDocument = new PdfDocument(reader, new PdfWriter(newFile),properties)) {
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
                pdfY = pageSize.getUpperRightY() - x - width;
                pdfX = pageSize.getUpperRightX() - y - height;
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


    public static void main(String[] args) throws IOException {
        PdfRedaction pdfRedaction = new PdfRedaction();



        File file = new File(ClassLoader.getSystemResource("INC5698057_1709663246939596.pdf").getPath());


        RedactionDTO redactionDTO = new RedactionDTO();


        //height
        //:
        //30
        //id
        //:
        //"5701f8bf-7ac3-4168-8dfd-f0830bcbd561"
        //width
        //:
        //207
        //x
        //:
        //22
        //y
        //:
        //37

        redactionDTO.setPage(1);
        RectangleDTO rectangleDTO1 = new RectangleDTO();
        rectangleDTO1.setX(0D);
        rectangleDTO1.setY(0D);
        rectangleDTO1.setWidth(300d);
        rectangleDTO1.setHeight(100d);
        rectangleDTO1.setId(UUID.randomUUID());

        RectangleDTO rectangleDTO2 = new RectangleDTO();
        rectangleDTO2.setX(400d);
        rectangleDTO2.setY(600d);
        rectangleDTO2.setWidth(200.83d);
        rectangleDTO2.setHeight(10.67d);
        rectangleDTO2.setId(UUID.randomUUID());



        //92.0	241.83	505.0	980.0

        redactionDTO.setRedactionId(UUID.fromString("f47839fa-ceeb-401a-9633-bed6e56899f0"));
        redactionDTO.setDocumentId(UUID.fromString("0c6a5d23-6dcb-45cf-a579-54ef0d0696ed"));
        redactionDTO.setRectangles(Set.of(rectangleDTO1, rectangleDTO2));

        //  redactionDTO.setRectangles(Set.of(rectangleDTO1));
//        height:27
//        id:"e57af9d9-a4d0-4e39-a501-235bcbca3d1c"
//        width: 231
//        x:230
//        y:297
        var newfile = pdfRedaction.redactPdf(file, List.of(redactionDTO));
        System.out.println(newfile.getAbsolutePath());
    }

}
