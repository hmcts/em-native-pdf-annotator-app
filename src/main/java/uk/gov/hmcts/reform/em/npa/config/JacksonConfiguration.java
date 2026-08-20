package uk.gov.hmcts.reform.em.npa.config;

import com.fasterxml.jackson.module.blackbird.BlackbirdModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
