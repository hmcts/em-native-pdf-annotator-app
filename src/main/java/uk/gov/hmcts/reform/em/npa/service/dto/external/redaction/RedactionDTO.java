package uk.gov.hmcts.reform.em.npa.service.dto.external.redaction;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode
@ToString
public class RedactionDTO {

    private int pageNumber;
    private int xCoordinate;
    private int yCoordinate;
    private int width;
    private int height;
}
