package uk.gov.hmcts.reform.em.npa.service.dto.redaction;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
public class RedactionSetDTO {

    @NotNull(message = "Search Redactions cannot be Blank")
    private Set<RedactionDTO> searchRedactions;

    public Set<RedactionDTO> getSearchRedactions() {
        return searchRedactions;
    }

}
