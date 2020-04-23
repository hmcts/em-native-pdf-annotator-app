package uk.gov.hmcts.reform.em.npa.ccd.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CcdDocument {

    @JsonProperty("document_url")
    private String documentUrl;

    @JsonProperty("document_binary_url")
    private String documentBinaryUrl;

    @JsonProperty("document_filename")
    private String documentFileName;

    @JsonCreator
    public CcdDocument(String documentUrl, String documentBinaryUrl, String documentFileName) {
        this.documentUrl = documentUrl;
        this.documentBinaryUrl = documentBinaryUrl;
        this.documentFileName = documentFileName;
    }
}
