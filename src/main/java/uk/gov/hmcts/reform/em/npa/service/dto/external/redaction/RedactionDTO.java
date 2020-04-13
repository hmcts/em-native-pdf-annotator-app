package uk.gov.hmcts.reform.em.npa.service.dto.external.redaction;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import javax.validation.constraints.NotBlank;
import java.util.UUID;

@Getter
@EqualsAndHashCode
@ToString
public class RedactionDTO {
    // Json ignore for which fields?
    @NotBlank(message = "UUID cannot be Blank")
    private UUID id;

    @NotBlank(message = "pageNumber cannot be Blank")
    private int pageNumber;

    @NotBlank(message = "xCoordinate cannot be Blank")
    private int xCoordinate;

    @NotBlank(message = "yCoordinateID cannot be Blank")
    private int yCoordinate;

    @NotBlank(message = "Width cannot be Blank")
    private int width;

    @NotBlank(message = "Height cannot be Blank")
    private int height;
}
