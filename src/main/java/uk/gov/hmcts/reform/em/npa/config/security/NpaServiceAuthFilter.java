package uk.gov.hmcts.reform.em.npa.config.security;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import uk.gov.hmcts.reform.authorisation.exceptions.InvalidTokenException;
import uk.gov.hmcts.reform.authorisation.exceptions.ServiceException;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;

import java.util.List;

public class NpaServiceAuthFilter extends AbstractPreAuthenticatedProcessingFilter {

    public static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";

    public static final String NOT_APPLICABLE = "N/A";

    private static final Logger LOG = LoggerFactory.getLogger(NpaServiceAuthFilter.class);

    private final List<String> authorisedServices;
    private final List<String> deleteAuthorisedServices;

    private final AuthTokenValidator authTokenValidator;

    public NpaServiceAuthFilter(
        AuthTokenValidator authTokenValidator,
        List<String> authorisedServices,
        List<String> deleteAuthorisedServices
    ) {
        this.authTokenValidator = authTokenValidator;
        if (authorisedServices == null || authorisedServices.isEmpty()) {
            throw new IllegalArgumentException("Must have at least one service defined");
        }
        this.authorisedServices = authorisedServices.stream().map(String::toLowerCase).toList();
        this.deleteAuthorisedServices = deleteAuthorisedServices.stream().map(String::toLowerCase).toList();
    }

    @Override
    protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
        try {
            String bearerToken = extractBearerToken(request);
            String serviceName = authTokenValidator.getServiceName(bearerToken);
            String service = serviceName == null ? null : serviceName.toLowerCase();

            if (service == null || !authorisedServices.contains(service)) {
                LOG.info("service forbidden {} for endpoint: {} method: {} ",
                        serviceName, request.getRequestURI(), request.getMethod());
                return null;
            }

            boolean isDeleteEndpoint = request.getRequestURI().contains("/api/redaction/document/")
                && "DELETE".equalsIgnoreCase(request.getMethod());

            if (isDeleteEndpoint && !deleteAuthorisedServices.contains(service)) {
                LOG.info("service forbidden {} for DELETE endpoint: {} method: {} ",
                        serviceName, request.getRequestURI(), request.getMethod());
                return null;
            }

            LOG.debug("service authorized {} for endpoint: {} method: {}",
                    serviceName, request.getRequestURI(), request.getMethod());
            return serviceName;

        } catch (InvalidTokenException | ServiceException exception) {
            LOG.warn("Unsuccessful service authentication", exception);
            return null;
        }
    }

    @Override
    protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
        return NOT_APPLICABLE;
    }

    private String extractBearerToken(HttpServletRequest request) {
        String token = request.getHeader(SERVICE_AUTHORIZATION);
        if (token == null) {
            throw new InvalidTokenException("ServiceAuthorization Token is missing");
        }
        return token.startsWith("Bearer ") ? token : "Bearer " + token;
    }
}
