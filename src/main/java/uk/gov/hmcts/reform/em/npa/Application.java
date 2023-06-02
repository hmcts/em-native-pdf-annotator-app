package uk.gov.hmcts.reform.em.npa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SuppressWarnings("HideUtilityClassConstructor") // Spring needs a constructor, its not a utility class
@SpringBootApplication(scanBasePackages = {"uk.gov.hmcts.reform.em.npa",
    "uk.gov.hmcts.reform.ccd.document.am",
    "uk.gov.hmcts.reform.authorisation",
    "uk.gov.hmcts.reform.idam.client"}
)
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
