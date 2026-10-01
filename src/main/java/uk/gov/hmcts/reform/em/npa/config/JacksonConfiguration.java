package uk.gov.hmcts.reform.em.npa.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.module.blackbird.BlackbirdModule;


@Configuration
public class JacksonConfiguration {

    /*
     * Jackson Afterburner module to speed up serialization/deserialization.
     */
    @Bean
    public BlackbirdModule blackbirdModule() {
        return new BlackbirdModule();
    }

}
