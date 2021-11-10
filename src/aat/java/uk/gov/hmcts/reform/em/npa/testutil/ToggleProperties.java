package uk.gov.hmcts.reform.em.npa.testutil;

//@ConfigurationProperties(prefix = "toggle")
public class ToggleProperties {

    private boolean cdamEnabled;

    public boolean isCdamEnabled() {
        return cdamEnabled;
    }

    public void setCdamEnabled(boolean cdamEnabled) {
        this.cdamEnabled = cdamEnabled;
    }
}
