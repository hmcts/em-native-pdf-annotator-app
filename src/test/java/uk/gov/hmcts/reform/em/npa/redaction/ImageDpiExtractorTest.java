package uk.gov.hmcts.reform.em.npa.redaction;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImageDpiExtractorTest {

    private ImageDpiExtractor imageDpiExtractor;

    @BeforeEach
    void setup() {
        imageDpiExtractor = new ImageDpiExtractor();
    }

    @Test
    void extractsCorrectDpiForStandardImage() throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            BufferedImage image = new BufferedImage(300, 300, BufferedImage.TYPE_INT_RGB);
            PDImageXObject pdImage = LosslessFactory.createFromImage(document, image);

            try (var contentStream = new PDPageContentStream(document, page)) {
                contentStream.drawImage(pdImage, 100, 110, 72, 115);
            }

            List<ImageDpiInfo> dpiInfos = imageDpiExtractor.extractDpi(page);

            assertEquals(1, dpiInfos.size());
            assertEquals(72, dpiInfos.getFirst().dpiX(), 0.1f);
            assertEquals(72, dpiInfos.getFirst().dpiY(), 0.1f);
        }
    }

    @Test
    void handlesMultipleImagesOnPage() throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            BufferedImage image1 = new BufferedImage(300, 300, BufferedImage.TYPE_INT_RGB);
            BufferedImage image2 = new BufferedImage(600, 600, BufferedImage.TYPE_INT_RGB);
            PDImageXObject pdImage1 = LosslessFactory.createFromImage(document, image1);
            PDImageXObject pdImage2 = LosslessFactory.createFromImage(document, image2);

            try (var contentStream = new PDPageContentStream(document, page)) {
                contentStream.drawImage(pdImage1, 0, 0, 72, 72);
                contentStream.drawImage(pdImage2, 100, 100, 144, 144);
            }

            List<ImageDpiInfo> dpiInfos = imageDpiExtractor.extractDpi(page);

            assertEquals(2, dpiInfos.size());
            assertEquals(72, dpiInfos.getFirst().dpiX(), 0.1f);
            assertEquals(72, dpiInfos.getFirst().dpiY(), 0.1f);
            assertEquals(72, dpiInfos.get(1).dpiX(), 0.1f);
            assertEquals(72, dpiInfos.get(1).dpiY(), 0.1f);
        }
    }
}
