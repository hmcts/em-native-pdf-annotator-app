package uk.gov.hmcts.reform.em.npa.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.gov.hmcts.reform.authorisation.exceptions.InvalidTokenException;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NpaServiceAuthFilterTest {

    private static final String EM_GW = "em_gw";
    private static final String DM_STORE = "dm_store";
    private static final String XUI_WEBAPP = "xui_webapp";

    private final List<String> authorisedServices = List.of(XUI_WEBAPP, DM_STORE, EM_GW);
    private final List<String> deleteAuthorisedServices = List.of(DM_STORE);

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;
    private final AuthTokenValidator authTokenValidator = mock(AuthTokenValidator.class);

    private final NpaServiceAuthFilter npaServiceAuthFilter =
        new NpaServiceAuthFilter(authTokenValidator, authorisedServices, deleteAuthorisedServices);

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);
    }

    @Test
    @DisplayName("XUI calling GET endpoint should be successful")
    void shouldContinueFilterChainWhenAuthorized() throws ServletException, IOException {
        request.addHeader("ServiceAuthorization", "Bearer validToken");
        request.setRequestURI("/api/markups");
        request.setMethod("GET");
        when(authTokenValidator.getServiceName("Bearer validToken")).thenReturn(XUI_WEBAPP);

        npaServiceAuthFilter.doFilterInternal(request, response, filterChain);

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should return 401 when service is not authorized")
    void shouldReturn401WhenNotAuthorized() throws ServletException, IOException {
        request.addHeader("ServiceAuthorization", "Bearer validToken");
        request.setRequestURI("/api/markups");
        request.setMethod("GET");
        when(authTokenValidator.getServiceName("Bearer validToken")).thenReturn("unknown_service");

        npaServiceAuthFilter.doFilterInternal(request, response, filterChain);

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("XUI calling DELETE endpoint should return 401")
    void shouldReturn401WhenNotAuthorizedForDelete() throws ServletException, IOException {
        request.addHeader("ServiceAuthorization", "Bearer validToken");
        request.setRequestURI("/api/redaction/document/123e4567-e89b-12d3-a456-426614174000");
        request.setMethod("DELETE");
        when(authTokenValidator.getServiceName("Bearer validToken")).thenReturn(XUI_WEBAPP);

        npaServiceAuthFilter.doFilterInternal(request, response, filterChain);

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("DM-Store calling DELETE endpoint should be successful")
    void shouldAllowDmStoreForDeleteEndpoint() throws ServletException, IOException {
        request.addHeader("ServiceAuthorization", "Bearer validToken");
        request.setRequestURI("/api/redaction/document/123e4567-e89b-12d3-a456-426614174000");
        request.setMethod("DELETE");
        when(authTokenValidator.getServiceName("Bearer validToken")).thenReturn(DM_STORE);

        npaServiceAuthFilter.doFilterInternal(request, response, filterChain);

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should return 401 when token is missing")
    void shouldReturn401WhenTokenMissing() throws ServletException, IOException {
        request.setRequestURI("/api/markups");
        request.setMethod("GET");

        npaServiceAuthFilter.doFilterInternal(request, response, filterChain);

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("Should return 401 when token is invalid")
    void shouldReturn401WhenTokenInvalid() throws ServletException, IOException {
        request.addHeader("ServiceAuthorization", "Bearer invalidToken");
        request.setRequestURI("/api/markups");
        request.setMethod("GET");
        when(authTokenValidator.getServiceName("Bearer invalidToken"))
            .thenThrow(new InvalidTokenException("Invalid Token"));

        npaServiceAuthFilter.doFilterInternal(request, response, filterChain);

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("Should handle token without Bearer prefix")
    void shouldHandleTokenWithoutBearerPrefix() throws ServletException, IOException {
        request.addHeader("ServiceAuthorization", "validToken");
        request.setRequestURI("/api/markups");
        request.setMethod("GET");
        when(authTokenValidator.getServiceName("Bearer validToken")).thenReturn(XUI_WEBAPP);

        npaServiceAuthFilter.doFilterInternal(request, response, filterChain);

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should throw exception when authorised services list is empty")
    void shouldThrowExceptionWhenNoAuthorisedServices() {
        Assertions.assertThrows(IllegalArgumentException.class, () ->
            new NpaServiceAuthFilter(authTokenValidator, List.of(), deleteAuthorisedServices));
    }

    @Test
    @DisplayName("Should throw exception when authorised services list is null")
    void shouldThrowExceptionWhenAuthorisedServicesNull() {
        Assertions.assertThrows(IllegalArgumentException.class, () ->
            new NpaServiceAuthFilter(authTokenValidator, null, deleteAuthorisedServices));
    }
}