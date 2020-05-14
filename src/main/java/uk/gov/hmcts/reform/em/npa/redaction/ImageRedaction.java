package uk.gov.hmcts.reform.em.npa.redaction;

import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RectangleDTO;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Set;

@Service
public class ImageRedaction {

    /**
     * Apply redaction to an image file
     *
     * @param imageFile the image to which the redactions are to be applied to
     * @param rectangles a list containing the information for the redactions to be applied to the image
     * @return the redacted image
     * @throws IOException
     */
    public File redactImage(File imageFile, Set<RectangleDTO> rectangles) throws IOException {
        BufferedImage img = ImageIO.read(imageFile);
        Graphics2D graph = img.createGraphics();

        rectangles.stream().forEach(redactionDTO -> {
            graph.setColor(Color.BLACK);
            graph.fill(new Rectangle2D.Double(
                    redactionDTO.getX(),
                    redactionDTO.getY(),
                    redactionDTO.getWidth(),
                    redactionDTO.getHeight()));
        });
        graph.dispose();

        String fileType = FilenameUtils.getExtension(imageFile.getName());
        final File alteredImage = File.createTempFile(
                String.format("Redacted-%s", FilenameUtils.getBaseName(imageFile.getName())), "." + fileType);
        ImageIO.write(img, fileType, alteredImage);
        return alteredImage;
    }
}
