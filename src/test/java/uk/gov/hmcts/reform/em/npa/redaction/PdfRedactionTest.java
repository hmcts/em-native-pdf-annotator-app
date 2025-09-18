package uk.gov.hmcts.reform.em.npa.redaction;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RectangleDTO;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RedactionDTO;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfRedactionTest {
    private static final File TEST_PDF_FILE_WITH_ERROR = new File(
            ClassLoader.getSystemResource("annotationTemplate.pdf").getPath()
    );

    private static final File TEST_PDF_FILE = new File(
            ClassLoader.getSystemResource("layered.pdf").getPath()
    );

    private static final File TEST_PDF_FILE_PASSWORD = new File(
            ClassLoader.getSystemResource("passwordprotected.pdf").getPath()
    );

    private PdfRedaction pdfRedaction;

    private List<RedactionDTO> redactions = new ArrayList<>();


    private File testPdfFile;

    @BeforeEach
    void setup() throws IOException {
        redactions = new ArrayList<>();
        initRedactionDTOList();
        pdfRedaction = new PdfRedaction();
        testPdfFile = File.createTempFile("TestPdf-", ".pdf");
    }

    @AfterEach
    void tearDown() {
        if (testPdfFile != null && testPdfFile.exists()) {
            assertTrue(testPdfFile.delete());
        }
    }

    private void createPdfWithRotation(int rotation) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            page.setRotation(rotation);
            document.addPage(page);
            document.save(testPdfFile);
        }
    }

    public void initRedactionDTOList() {
        for (int i = 0; i < 5; i++) {
            redactions.add(createTestRedaction(i + 1, 100.00,100.00,100.00,100.00));
        }
    }

    private RedactionDTO createTestRedaction(int pageNumber, double x, double y, double w, double h) {
        RedactionDTO redaction = new RedactionDTO();
        redaction.setRedactionId(UUID.randomUUID());
        redaction.setDocumentId(UUID.randomUUID());
        redaction.setPage(pageNumber);

        RectangleDTO rect = new RectangleDTO();
        rect.setId(UUID.randomUUID());
        rect.setX(x);
        rect.setY(y);
        rect.setWidth(w);
        rect.setHeight(h);

        redaction.setRectangles(new HashSet<>(Collections.singletonList(rect)));
        return redaction;
    }

    @Test
    void pdfRedactionTest() throws IOException {
        File result = pdfRedaction.redactPdf(TEST_PDF_FILE, redactions);
        assertTrue(result.getName().contains("Redacted-layered"));
        assertTrue(result.getName().contains(".pdf"));
    }

    @Test
    void pdfRedactionPasswordTest() throws IOException {
        File result = pdfRedaction.redactPdf(TEST_PDF_FILE_PASSWORD, redactions);
        assertTrue(result.getName().contains("Redacted-passwordprotected"));
        assertTrue(result.getName().contains(".pdf"));
    }

    @Test
    void pdfRedactionFailureTest() {
        assertThrows(IOException.class, () ->
                pdfRedaction.redactPdf(new File("invalid_file"), redactions));
    }

    @Test
    void shouldHandleEmptyRedactionList() throws IOException {
        File result = pdfRedaction.redactPdf(TEST_PDF_FILE, Collections.emptyList());
        assertTrue(result.exists());
    }

    @Test
    void shouldHandleRedactionWithNoRectangles() throws IOException {
        RedactionDTO redaction = new RedactionDTO();
        redaction.setPage(1);
        redaction.setRectangles(Collections.emptySet());

        File result = pdfRedaction.redactPdf(TEST_PDF_FILE, List.of(redaction));
        assertTrue(result.exists());
    }

    @Test
    void shouldDoRedactionWithRetry() throws IOException {
        RedactionDTO redaction = new RedactionDTO();
        redaction.setPage(1);
        redaction.setRectangles(Collections.emptySet());

        File result = pdfRedaction.redactPdf(TEST_PDF_FILE_WITH_ERROR, List.of(redaction));
        assertTrue(result.exists());
    }

    @Test
    void shouldCreateRectangleCorrectlyForRotatedPage() throws IOException {
        // Simulate a rotated page
        File tempPdf = File.createTempFile("rotated", ".pdf");
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            page.setRotation(90); // trigger the rotated logic
            document.addPage(page);
            document.save(tempPdf);
        }

        RectangleDTO rectangle = new RectangleDTO();
        rectangle.setX(50.0);
        rectangle.setY(50.0);
        rectangle.setHeight(100.0);
        rectangle.setWidth(100.0);

        RedactionDTO redaction = new RedactionDTO();
        redaction.setPage(1);
        redaction.setRectangles(Set.of(rectangle));

        File result = pdfRedaction.redactPdf(tempPdf, List.of(redaction));
        assertTrue(result.exists());
    }

    @ParameterizedTest(name = "for {0} degree rotation")
    @ValueSource(ints = {90, 180, 270})
    void redactPdf_WithPageRotation_AppliesRedaction(int rotation) throws IOException {
        createPdfWithRotation(rotation);
        RedactionDTO redaction = createTestRedaction(1, 100, 200, 50, 30);

        File result = pdfRedaction.redactPdf(testPdfFile, List.of(redaction));

        assertRedactionApplied(result);
    }

    private void assertRedactionApplied(File result) {
        assertNotNull(result);
        assertTrue(result.exists());
        assertTrue(result.length() > 0, "Redacted file should not be empty");
    }

    @Test
    void redactPdf_MultipleRotations_DifferentPages() throws IOException {
        // Create PDF with pages having different rotations
        try (PDDocument doc = new PDDocument()) {
            PDPage page1 = new PDPage(PDRectangle.A4);
            page1.setRotation(90);
            doc.addPage(page1);

            PDPage page2 = new PDPage(PDRectangle.A4);
            page2.setRotation(180);
            doc.addPage(page2);

            doc.save(testPdfFile);
        }

        List<RedactionDTO> redactionDTOS = Arrays.asList(
                createTestRedaction(1, 100, 200, 50, 30),
                createTestRedaction(2, 150, 250, 75, 45)
        );

        File result = pdfRedaction.redactPdf(testPdfFile, redactionDTOS);

        assertRedactionApplied(result);
    }
}