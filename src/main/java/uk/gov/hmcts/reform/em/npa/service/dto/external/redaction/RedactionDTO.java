package uk.gov.hmcts.reform.em.npa.service.dto.external.redaction;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

@Getter
@EqualsAndHashCode
@ToString
public class RedactionDTO {
    // Json ignore for which fields?

    private UUID id;
    private int pageNumber;
    private int xCoordinate;
    private int yCoordinate;
    private int width;
    private int height;
}
