package uk.gov.hmcts.reform.em.npa.testutil;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "toggles")
public class ToggleProperties {

    private boolean cdamEnabled;

    private boolean searchMarkupsEnabled;

    public boolean isCdamEnabled() {
        return cdamEnabled;
    }

    public void setCdamEnabled(boolean cdamEnabled) {
        this.cdamEnabled = cdamEnabled;
    }

    public boolean isSearchMarkupsEnabled() {
        return searchMarkupsEnabled;
    }

    public void setSearchMarkupsEnabled(boolean searchMarkupsEnabled) {
        this.searchMarkupsEnabled = searchMarkupsEnabled;
    }
}
