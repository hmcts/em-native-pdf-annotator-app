package uk.gov.hmcts.reform.em.npa.config.security;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import uk.gov.hmcts.reform.em.npa.repository.IdamRepository;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SecurityUtilsTest {

    private IdamRepository idamRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @Mock
    private UserDetails userDetails;

    @Mock
    private JwtAuthenticationToken jwtAuthenticationToken;

    @Mock
    private DefaultOidcUser defaultOidcUser;

    private SecurityUtils securityUtils;

    @BeforeEach
    public void setup() {

        MockitoAnnotations.openMocks(this);
        SecurityContextHolder.setContext(securityContext);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        securityUtils = new SecurityUtils(idamRepository);
    }

    @Test
    void getCurrentUserLoginNoAuthentication() {
        Assert.assertFalse(securityUtils.getCurrentUserLogin().isPresent());
    }

    @Test
    void getCurrentUserLoginUserDetails() {

        Mockito.when(authentication.getPrincipal()).thenReturn(userDetails);
        Mockito.when(userDetails.getUsername()).thenReturn("aabb");

        Assert.assertEquals("aabb",  securityUtils.getCurrentUserLogin().get());
    }

    @Test
    void getCurrentUserLoginString() {

        Mockito.when(authentication.getPrincipal()).thenReturn("xyz");

        assertEquals("xyz",  securityUtils.getCurrentUserLogin().get());
    }

    @Test
    void getCurrentUserLoginDefaultOidcUser() {

        Mockito.when(authentication.getPrincipal()).thenReturn(defaultOidcUser);
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("preferred_username","testUser");
        Mockito.when(defaultOidcUser.getAttributes()).thenReturn(attributes);
        assertEquals("testUser", securityUtils.getCurrentUserLogin().get());
    }
}
