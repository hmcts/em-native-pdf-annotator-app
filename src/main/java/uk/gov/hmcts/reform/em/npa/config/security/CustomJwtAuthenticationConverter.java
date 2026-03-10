package uk.gov.hmcts.reform.em.npa.config.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import uk.gov.hmcts.reform.em.npa.repository.IdamRepository;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import static org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames.ACCESS_TOKEN;

public class CustomJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final Logger LOG = LoggerFactory.getLogger(CustomJwtAuthenticationConverter.class);

    public static final String TOKEN_NAME = "tokenName";
    public static final String USER_ID_CLAIM = "userId";

    private final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
    private final IdamRepository idamRepository;

    public CustomJwtAuthenticationConverter(IdamRepository idamRepository) {
        this.idamRepository = idamRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        var authorities = jwtGrantedAuthoritiesConverter.convert(jwt);
        
        if (jwt.hasClaim(TOKEN_NAME) && jwt.getClaim(TOKEN_NAME).equals(ACCESS_TOKEN)) {
            LOG.info("Fetching user info from IDAM during JWT authentication");
            UserInfo userInfo = idamRepository.getUserInfo(jwt.getTokenValue());
            String userId = userInfo.getUid();
            LOG.info("User authenticated with userId: {}", userId);
            
            return new JwtAuthenticationToken(jwt, authorities, userId);
        }
        
        return new JwtAuthenticationToken(jwt, authorities);
    }
}
