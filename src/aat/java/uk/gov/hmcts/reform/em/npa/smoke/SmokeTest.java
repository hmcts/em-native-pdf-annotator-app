package uk.gov.hmcts.reform.em.npa.smoke;

import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import net.serenitybdd.rest.SerenityRest;
import net.thucydides.core.annotations.WithTag;
import net.thucydides.core.annotations.WithTags;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.em.EmTestConfig;
import uk.gov.hmcts.reform.em.npa.Application;
import uk.gov.hmcts.reform.em.npa.testutil.TestUtil;

import java.util.Map;

@SpringBootTest(classes = {TestUtil.class, EmTestConfig.class, Application.class})
@EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class,HibernateJpaAutoConfiguration.class})
@TestPropertySource(value = "classpath:application.yml")
@RunWith(SpringIntegrationSerenityRunner.class)
@WithTags({@WithTag("testType:Smoke")})
public class SmokeTest {

    private static final String MESSAGE = "Welcome to Native PDF Annotator API!";

    @Value("${test.url}")
    private String testUrl;

    @Test
    public void testHealthEndpoint() {

        SerenityRest.useRelaxedHTTPSValidation();

        Map responseMap =
                SerenityRest
                        .given()
                        .baseUri(testUrl)
                        .get("/")
                        .then()
                        .statusCode(200)
                        .extract()
                        .body()
                        .as(Map.class);

        Assert.assertEquals(1, responseMap.size());
        Assert.assertEquals(MESSAGE, responseMap.get("message"));

    }
}
