package uk.gov.hmcts.reform.em.npa.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


import javax.validation.constraints.NotBlank;
import java.util.UUID;

@Setter
@Getter
@EqualsAndHashCode
@ToString
public class MarkUpDTO {
    // Json ignore for which fields?
    @NotBlank(message = "UUID cannot be Blank")
    private UUID id;

    @NotBlank(message = "documentId cannot be Blank")
    private UUID documentId;

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
