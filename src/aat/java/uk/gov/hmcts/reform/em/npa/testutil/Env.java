package uk.gov.hmcts.reform.em.npa.testutil;

import org.apache.commons.lang3.Validate;

import java.util.Properties;

public class Env {

    static Properties defaults = new Properties();

    static {
        defaults.setProperty("PROXY", "false");
        defaults.setProperty("TEST_URL", "http://localhost:8080");
        defaults.setProperty("EM_ANNO_APP_URL", "http://localhost:4623");
        defaults.setProperty("DM_STORE_APP_URL", "http://localhost:4603");
    }

    public static String getUseProxy() { return require("PROXY"); }

    public static String getTestUrl() {
        return require("TEST_URL");
    }

    public static String getAnnotationApiUrl() {
        return require("EM_ANNO_APP_URL");
    }

    public static String getDmApiUrl() {
        return require("DM_STORE_APP_URL");
    }

    public static String require(String name) {
        return Validate.notNull(System.getenv(name) == null ? defaults.getProperty(name) : System.getenv(name), "Environment variable `%s` is required", name);
    }
}
