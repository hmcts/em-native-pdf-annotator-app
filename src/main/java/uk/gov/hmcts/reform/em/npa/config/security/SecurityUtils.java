package uk.gov.hmcts.reform.em.npa.config.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Utility class for Spring Security.
 */
@Service
public class SecurityUtils {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityUtils.class);

    /**
     * Get the login of the current user.
     *
     * @return the login of the current user
     */
    public Optional<String> getCurrentUserLogin() {
        SecurityContext securityContext = SecurityContextHolder.getContext();

        return Optional.ofNullable(securityContext.getAuthentication())
            .map(authentication -> switch (authentication) {
                case Authentication auth when auth.getPrincipal() instanceof UserDetails userDetails ->
                    userDetails.getUsername();

                case Authentication auth when auth.getPrincipal() instanceof String s -> s;

                case JwtAuthenticationToken jwtAuth -> {
                    String userId = jwtAuth.getName();
                    LOG.debug("Retrieved userId from SecurityContext (no IDAM call): {}", userId);
                    yield userId;
                }

                case Authentication auth when auth.getPrincipal() instanceof DefaultOidcUser oidcUser ->
                    (String) oidcUser.getAttributes().get("preferred_username");

                default -> null;
            });
    }

}
