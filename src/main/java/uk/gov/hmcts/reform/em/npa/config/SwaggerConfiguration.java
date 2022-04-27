package uk.gov.hmcts.reform.em.npa.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfiguration {

    @Bean
    public OpenAPI api() {
        return new OpenAPI()
                .info(
                        new Info().title("EM (NPA) Redactions")
                                .description("API to facilitate Document Redaction")
                                .version("v1.0.1"));
    }
}
