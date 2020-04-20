package uk.gov.hmcts.reform.em.npa.redaction;

import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.em.npa.domain.RedactionDTO;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

@Service
public class ImageRedaction {

    /**
     * Apply redaction to an image file
     *
     * @param imageFile the image to which the redactions are to be applied to
     * @param redactionDTOList a list containing the information for the redactions to be applied to the image
     * @return the redacted image
     * @throws IOException
     */
    public File redaction(File imageFile, List<RedactionDTO> redactionDTOList) throws IOException {
        BufferedImage img = ImageIO.read(imageFile);
        Graphics2D graph = img.createGraphics();

        redactionDTOList.stream().forEach(redactionDTO -> {
            graph.setColor(Color.BLACK);
            graph.fill(new Rectangle(
                    redactionDTO.getXCoordinate(),
                    redactionDTO.getYCoordinate(),
                    redactionDTO.getWidth(),
                    redactionDTO.getHeight()));
        });
        graph.dispose();

        String fileType = FilenameUtils.getExtension(imageFile.getName());
        final File alteredImage = File.createTempFile("altered", "." + fileType);
        ImageIO.write(img, fileType, alteredImage);
        return alteredImage;
    }
}
