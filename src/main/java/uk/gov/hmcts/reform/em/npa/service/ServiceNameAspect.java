package uk.gov.hmcts.reform.em.npa.service;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.gov.hmcts.reform.authorisation.exceptions.InvalidTokenException;
import uk.gov.hmcts.reform.em.npa.config.security.SecurityUtils;

import javax.servlet.http.HttpServletRequest;

@Aspect
@Component
public class ServiceNameAspect {

    private final Logger log = LoggerFactory.getLogger(ServiceNameAspect.class);

    private static final String BEARER = "Bearer ";

    @Autowired
    SecurityUtils securityUtils;

    @Before(
        " execution(* uk.gov.hmcts.reform.em.npa.rest.MarkUpResource.*(..)) ||"
            + " execution(* uk.gov.hmcts.reform.em.npa.rest.RedactionResource.*(..))")
    public void logServiceName() {

        HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String s2sToken = request.getHeader("serviceauthorization");
        if (StringUtils.isNotBlank(s2sToken)) {
            try {
                String serviceName;
                if (s2sToken.startsWith(BEARER)) {
                    serviceName = securityUtils.getServiceName(s2sToken);
                } else {
                    serviceName = securityUtils.getServiceName(BEARER + s2sToken);
                }
                log.info("em-npa : Endpoint : {}  for : {} method is accessed by {} ", request.getRequestURI(),
                        request.getMethod(), serviceName);
            } catch (InvalidTokenException invalidTokenException) {
                log.warn("invalidTokenException logged is: {} ", invalidTokenException.getMessage());
            }
        }
    }
}
