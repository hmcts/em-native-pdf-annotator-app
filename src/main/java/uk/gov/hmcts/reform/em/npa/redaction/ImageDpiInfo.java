package uk.gov.hmcts.reform.em.npa.redaction;

public class ImageDpiInfo {
    public final String name;
    public final float dpiX;
    public final float dpiY;

    public ImageDpiInfo(String name, float dpiX, float dpiY) {
        this.name = name;
        this.dpiX = dpiX;
        this.dpiY = dpiY;
    }
}
