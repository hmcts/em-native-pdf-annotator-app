package uk.gov.hmcts.reform.em.npa.ccd.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@NoArgsConstructor
public class CcdEvent {
    @JsonProperty("id")
    private String eventId;

    public CcdEvent(String eventId) {
        this.eventId = eventId;
    }

    public String getEventId() {
        return eventId;
    }
}
