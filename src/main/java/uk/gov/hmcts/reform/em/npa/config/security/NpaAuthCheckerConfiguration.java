package uk.gov.hmcts.reform.em.npa.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.authorisation.validators.ServiceAuthTokenValidator;

import java.util.List;

@Configuration
public class NpaAuthCheckerConfiguration {

    @Value("#{'${idam.s2s-authorised.services}'.split(',')}")
    private List<String> authorisedServices;

    @Value("#{'${idam.s2s-authorised.delete-endpoint-services}'.split(',')}")
    private List<String> deleteAuthorisedServices;

    @Bean
    @ConditionalOnProperty("idam.s2s-authorised.services")
    public NpaServiceAuthFilter npaServiceAuthFilter(ServiceAuthorisationApi authorisationApi) {
        AuthTokenValidator authTokenValidator = new ServiceAuthTokenValidator(authorisationApi);
        return new NpaServiceAuthFilter(
                authTokenValidator,
                authorisedServices,
                deleteAuthorisedServices
        );
    }

    @Bean
    @ConditionalOnProperty("idam.s2s-authorised.services")
    public FilterRegistrationBean<NpaServiceAuthFilter> npaServiceAuthFilterRegistration(
            NpaServiceAuthFilter filter) {
        FilterRegistrationBean<NpaServiceAuthFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}