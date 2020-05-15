package uk.gov.hmcts.reform.em.npa.service.dto.redaction;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Setter
@Getter
@EqualsAndHashCode
@ToString
public class RedactionDTO implements Serializable {

    @NotNull(message = "Redaction Id cannot be Blank")
    private UUID redactionId;

    @NotNull(message = "Document Id cannot be Blank")
    private UUID documentId;

    @NotNull(message = "Page Number cannot be Blank")
    private Integer page;

    private Set<RectangleDTO> rectangles = new HashSet<>();
}
