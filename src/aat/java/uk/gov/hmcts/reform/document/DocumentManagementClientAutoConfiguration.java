package uk.gov.hmcts.reform.document;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/**
 * AAT compatibility configuration for document-management-client 7.0.1.
 *
 * The published client is compiled against the Boot 3 health API. Its
 * configuration is discovered by em-test-helper's component scan, so loading
 * the original class fails before the AAT context starts. The document Feign
 * clients remain required by DmHelper; the incompatible health-indicator bean
 * is deliberately not registered here.
 */
@Configuration
@ConditionalOnProperty(prefix = "document_management", name = "url")
@EnableFeignClients(basePackages = "uk.gov.hmcts.reform.document")
public class DocumentManagementClientAutoConfiguration {
}
