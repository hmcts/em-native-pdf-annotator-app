package uk.gov.hmcts.reform.em.npa.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import uk.gov.hmcts.reform.authorisation.exceptions.InvalidTokenException;
import uk.gov.hmcts.reform.authorisation.exceptions.ServiceException;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class NpaServiceAuthFilter extends OncePerRequestFilter {

    public static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";

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
        if (CollectionUtils.isEmpty(authorisedServices)) {
            throw new IllegalArgumentException("Must have at least one service defined");
        }
        this.authorisedServices = authorisedServices.stream().map(String::toLowerCase).toList();
        this.deleteAuthorisedServices = deleteAuthorisedServices.stream().map(String::toLowerCase).toList();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String bearerToken = extractBearerToken(request);
            String serviceName = authTokenValidator.getServiceName(bearerToken);
            String service = Objects.nonNull(serviceName) ? serviceName.toLowerCase() : null;

            if (Objects.isNull(service) || !authorisedServices.contains(service)) {
                LOG.info("service forbidden {} for endpoint: {} method: {}",
                        serviceName, request.getRequestURI(), request.getMethod());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            boolean isDeleteEndpoint = request.getRequestURI().contains("/api/redaction/document/")
                    && "DELETE".equalsIgnoreCase(request.getMethod());

            if (isDeleteEndpoint && !deleteAuthorisedServices.contains(service)) {
                LOG.info("service forbidden {} for DELETE endpoint: {} method: {}, deleteAuthorisedServices: {}",
                        serviceName, request.getRequestURI(), request.getMethod(), deleteAuthorisedServices);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            LOG.debug("service authorized {} for endpoint: {} method: {}",
                    serviceName, request.getRequestURI(), request.getMethod());

            filterChain.doFilter(request, response);

        } catch (InvalidTokenException | ServiceException exception) {
            LOG.warn("Unsuccessful service authentication", exception);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    private String extractBearerToken(HttpServletRequest request) {
        String token = request.getHeader(SERVICE_AUTHORIZATION);
        if (Objects.isNull(token)) {
            throw new InvalidTokenException("ServiceAuthorization Token is missing");
        }
        return token.startsWith("Bearer ") ? token : "Bearer " + token;
    }
}