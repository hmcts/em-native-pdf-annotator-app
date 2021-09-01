package uk.gov.hmcts.reform.em.npa.testutil;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "toggle")
public class ToggleProperties {

    private boolean enableDocumentTaskEndpoint;

    public boolean isEnableDocumentTaskEndpoint() {
        return this.enableDocumentTaskEndpoint;
    }

    public void setEnableDocumentTaskEndpoint(boolean enableDocumentTaskEndpoint) {
        this.enableDocumentTaskEndpoint = enableDocumentTaskEndpoint;
    }

    public String toString() {
        return "ToggleProperties(enableDocumentTaskEndpoint="
                + this.isEnableDocumentTaskEndpoint() + ")";
    }
}
