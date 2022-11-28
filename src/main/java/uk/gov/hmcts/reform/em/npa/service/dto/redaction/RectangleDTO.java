package uk.gov.hmcts.reform.em.npa.service.dto.redaction;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.UUID;

/**
 * A DTO for the Rectangle entity.
 */
@Setter
@Getter
@EqualsAndHashCode
@ToString
public class RectangleDTO implements Serializable {

    @NotNull(message = "UUID cannot be Blank")
    private UUID id;

    @NotNull(message = "xcoordinate cannot be Blank")
    private Double x;

    @NotNull(message = "ycoordinate cannot be Blank")
    private Double y;

    @NotNull(message = "Width cannot be Blank")
    private Double width;

    @NotNull(message = "Height cannot be Blank")
    private Double height;

}
