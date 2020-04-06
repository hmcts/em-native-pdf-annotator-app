package uk.gov.hmcts.reform.em.npa.redaction;

import org.apache.pdfbox.tools.imageio.ImageIOUtil;
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
        graph.fill(
                new Rectangle(
                        redactionDTO.getxCoordinate(),
                        redactionDTO.getyCoordinate(),
                        redactionDTO.getWidth(),
                        redactionDTO.getHeight()
                )
        );
//      e.g: graph.fill(new Rectangle(100, 100, 100, 100));
        graph.dispose();

        // do we need to flatten image?
        final File alteredImage = File.createTempFile("altered", ".png");
        ImageIOUtil.writeImage(img, alteredImage.getName(), 300);
//        ImageIO.write(img, "png", alteredImage);
        return alteredImage;
    }
}
