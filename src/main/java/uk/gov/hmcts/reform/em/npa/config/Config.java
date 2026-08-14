package uk.gov.hmcts.reform.em.npa.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.ccd.document.am.feign.CaseDocumentClientApi;
import uk.gov.hmcts.reform.idam.client.IdamApi;

@Configuration
@EnableFeignClients(basePackageClasses = {CaseDocumentClientApi.class, IdamApi.class, ServiceAuthorisationApi.class})
public class Config {
}
