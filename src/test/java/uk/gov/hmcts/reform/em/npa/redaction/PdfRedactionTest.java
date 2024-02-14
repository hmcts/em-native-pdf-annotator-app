package uk.gov.hmcts.reform.em.npa.redaction;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RectangleDTO;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RedactionDTO;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public class PdfRedactionTest {
    private static final File TEST_PDF_FILE = new File(
            ClassLoader.getSystemResource("layered.pdf").getPath()
    );

    private static final File TEST_PDF_FILE_PASSWORD = new File(
        ClassLoader.getSystemResource("passwordprotected.pdf").getPath()
    );

    private PdfRedaction pdfRedaction = new PdfRedaction();

    private List<RedactionDTO> redactions = new ArrayList<>();

    @Before
    public void setup() {
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
    public void pdfRedactionTest() throws IOException {
        File result = pdfRedaction.redactPdf(TEST_PDF_FILE, redactions);
        Assert.assertTrue(result.getName().contains("Redacted-layered"));
        Assert.assertTrue(result.getName().contains(".pdf"));
    }

    @Test
    public void pdfRedactionPasswordTest() throws IOException {
        File result = pdfRedaction.redactPdf(TEST_PDF_FILE_PASSWORD, redactions);
        Assert.assertTrue(result.getName().contains("Redacted-passwordprotected"));
        Assert.assertTrue(result.getName().contains(".pdf"));
    }

    @Test(expected = IOException.class)
    public void pdfRedactionFailureTest() throws IOException {
        pdfRedaction.redactPdf(new File("invalid_file"), redactions);
    }
}
