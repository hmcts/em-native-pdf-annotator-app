package uk.gov.hmcts.reform.em.npa.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan("uk.gov.hmcts.reform.em.npa.rest")
public class SwaggerConfiguration {

    public OpenAPI api() {
        return new OpenAPI()
                .info(
                        new Info().title("EM Native PDF Annotator App")
                                .description("API to burn annotations onto a PDF and facilitate Document Redaction")
                                .version("v1.0.1")
                );
    }
}
