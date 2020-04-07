package uk.gov.hmcts.reform.em.npa.redaction;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.npa.service.dto.external.redaction.RedactionDTO;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@Service
public class ImageRedaction {

    public File redaction(File imageFile, RedactionDTO redactionDTO) throws IOException {
        BufferedImage img = ImageIO.read(imageFile);
        Graphics2D graph = img.createGraphics();

        graph.setColor(Color.BLACK);
        graph.fill(new Rectangle(
                        redactionDTO.getXCoordinate(),
                        redactionDTO.getYCoordinate(),
                        redactionDTO.getWidth(),
                        redactionDTO.getHeight()));
        graph.dispose();

        final File alteredImage = File.createTempFile("altered", ".png");
        ImageIO.write(img, "png", alteredImage);
        return alteredImage;
    }
}
