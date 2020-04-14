package uk.gov.hmcts.reform.em.npa.config.security;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

public class SecurityUtilsTest {

    @Mock
    private SecurityUtils securityUtils;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void getCurrentUserLoginNoAuthentication() {
        Assert.assertFalse(securityUtils.getCurrentUserLogin().isPresent());
    }

    @Ignore
    @Test
    public void getCurrentUserLogin() {
        Authentication authentication = Mockito.mock(Authentication.class);
        UserDetails userDetails = Mockito.mock(UserDetails.class);
        Mockito.when(authentication.getPrincipal()).thenReturn(userDetails);
        Mockito.when(userDetails.getUsername()).thenReturn("aabb");
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        securityContext.setAuthentication(authentication);
        Mockito.when(SecurityContextHolder.getContext()).thenReturn(securityContext);
//        SecurityContextHolder.getContext().setAuthentication(authentication);
        Assert.assertEquals("aabb",  securityUtils.getCurrentUserLogin().get());
    }
}