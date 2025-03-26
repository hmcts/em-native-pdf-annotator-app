package uk.gov.hmcts.reform.em.npa.redaction;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RectangleDTO;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RedactionDTO;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfRedactionTest {
    private static final File TEST_PDF_FILE = new File(
            ClassLoader.getSystemResource("layered.pdf").getPath()
    );

    private static final File TEST_PDF_FILE_PASSWORD = new File(
            ClassLoader.getSystemResource("passwordprotected.pdf").getPath()
    );

    private final PdfRedaction pdfRedaction = new PdfRedaction();

    private List<RedactionDTO> redactions = new ArrayList<>();

    @BeforeEach
    public void setup() {
        redactions = new ArrayList<>();
        initRedactionDTOList();
    }

    public void initRedactionDTOList() {
        for (int i = 0; i < 5; i++) {
            RedactionDTO redaction = new RedactionDTO();
            redaction.setRedactionId(UUID.randomUUID());
            redaction.setDocumentId(UUID.randomUUID());
            redaction.setPage(i + 1);

            RectangleDTO rectangle = new RectangleDTO();
            rectangle.setId(UUID.randomUUID());
            rectangle.setX(100.00);
            rectangle.setY(100.00);
            rectangle.setHeight(100.00);
            rectangle.setWidth(100.00);

            redaction.setRectangles(new HashSet<>(Collections.singletonList(rectangle)));

            redactions.add(redaction);
        }
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
        rectangle.setX(50);
        rectangle.setY(50);
        rectangle.setHeight(100);
        rectangle.setWidth(100);

        RedactionDTO redaction = new RedactionDTO();
        redaction.setPage(1);
        redaction.setRectangles(Set.of(rectangle));

        File result = pdfRedaction.redactPdf(tempPdf, List.of(redaction));
        assertTrue(result.exists());
    }

    @Test
    void shouldHandleRedactionWithNullRectangleSet() throws IOException {
        RedactionDTO redaction = new RedactionDTO();
        redaction.setPage(1);
        redaction.setRectangles(null); // defensive check

        File result = pdfRedaction.redactPdf(TEST_PDF_FILE, List.of(redaction));
        assertTrue(result.exists());
    }
}