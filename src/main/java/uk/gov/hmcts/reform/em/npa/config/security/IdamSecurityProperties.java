package uk.gov.hmcts.reform.em.npa.config.security;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
@Component
@ConfigurationProperties(prefix = "idam.security")
public class IdamSecurityProperties {
    private List<String> allowedIssuers = new ArrayList<>();
}