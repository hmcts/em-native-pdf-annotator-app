package uk.gov.hmcts.reform.em.npa.redaction;

import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RectangleDTO;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


class ImageRedactionTest {

    private static final File TEST_IMAGE_FILE = new File(
            ClassLoader.getSystemResource("fist.png").getPath()
    );

    private ImageRedaction imageRedaction = new ImageRedaction();

    private Set<RectangleDTO> rectangles = new HashSet<>();

    @BeforeEach
    void setup() {
        initRedactionDTOList();
    }

    public void initRedactionDTOList() {
        for (int i = 0; i < 5; i++) {
            RectangleDTO rectangle = new RectangleDTO();
            rectangle.setId(UUID.randomUUID());
            rectangle.setX(100.00);
            rectangle.setY(100.00);
            rectangle.setHeight(100.00);
            rectangle.setWidth(100.00);

            rectangles.add(rectangle);
        }
    }

    @Test
    void imageRedactionTest() throws IOException {
        File result = imageRedaction.redactImage(TEST_IMAGE_FILE, rectangles);
        assertTrue(result.getName().contains("Redacted-fist"));
        assertTrue(result.getName().contains(FilenameUtils.getExtension(TEST_IMAGE_FILE.getName())));
    }

    @Test
    void imageRedactionFailureTest() {
        assertThrows(IOException.class, () ->
            imageRedaction.redactImage(new File("invalid_file"), rectangles));
    }
}
