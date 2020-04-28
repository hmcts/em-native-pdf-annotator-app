package uk.gov.hmcts.reform.em.npa.service.dto.redaction;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.NotNull;
import java.util.UUID;

@Setter
@Getter
@EqualsAndHashCode
@ToString
public class MarkUpDTO {
    @NotNull(message = "UUID cannot be Blank")
    private UUID id;

    @NotNull(message = "documentId cannot be Blank")
    private UUID documentId;

    @NotNull(message = "pageNumber cannot be Blank")
    private int pageNumber;

    @NotNull(message = "xcoordinate cannot be Blank")
    private Integer xcoordinate;

    @NotNull(message = "yCoordinateID cannot be Blank")
    private Integer ycoordinate;

    @NotNull(message = "Width cannot be Blank")
    private Integer width;

    @NotNull(message = "Height cannot be Blank")
    private Integer height;
}
