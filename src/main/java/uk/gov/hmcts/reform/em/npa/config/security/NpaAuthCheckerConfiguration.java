package uk.gov.hmcts.reform.em.npa.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.authorisation.validators.ServiceAuthTokenValidator;

import java.util.Collections;
import java.util.List;

@Configuration
public class NpaAuthCheckerConfiguration {

    @Value("#{'${idam.s2s-authorised.services}'.split(',')}")
    private List<String> authorisedServices;

    @Value("#{'${idam.s2s-authorised.can-delete-services}'.split(',')}")
    private List<String> deleteAuthorisedServices;

    @Bean
    @ConditionalOnProperty("idam.s2s-authorised.services")
    public NpaServiceAuthFilter npaServiceAuthFilter(ServiceAuthorisationApi authorisationApi,
                                                     AuthenticationManager authenticationManager) {
        AuthTokenValidator authTokenValidator = new ServiceAuthTokenValidator(authorisationApi);
        NpaServiceAuthFilter npaServiceAuthFilter = new NpaServiceAuthFilter(
            authTokenValidator,
            authorisedServices,
            deleteAuthorisedServices
        );
        npaServiceAuthFilter.setAuthenticationManager(authenticationManager);
        return npaServiceAuthFilter;
    }

    @Bean
    @ConditionalOnProperty("idam.s2s-authorised.services")
    public FilterRegistrationBean<NpaServiceAuthFilter> npaServiceAuthFilterRegistration(
        NpaServiceAuthFilter filter) {
        FilterRegistrationBean<NpaServiceAuthFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    @ConditionalOnMissingBean(name = "preAuthenticatedAuthenticationProvider")
    public PreAuthenticatedAuthenticationProvider preAuthenticatedAuthenticationProvider() {
        PreAuthenticatedAuthenticationProvider preAuthenticatedAuthenticationProvider =
            new PreAuthenticatedAuthenticationProvider();
        preAuthenticatedAuthenticationProvider.setPreAuthenticatedUserDetailsService(
            token -> new User((String) token.getPrincipal(), "N/A", Collections.emptyList())
        );
        return preAuthenticatedAuthenticationProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
        PreAuthenticatedAuthenticationProvider preAuthenticatedAuthenticationProvider) {
        return new ProviderManager(Collections.singletonList(preAuthenticatedAuthenticationProvider));
    }
}
