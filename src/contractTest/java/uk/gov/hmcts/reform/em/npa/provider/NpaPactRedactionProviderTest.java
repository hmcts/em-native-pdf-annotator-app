

package uk.gov.hmcts.reform.em.npa.provider;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.PactBrokerConsumerVersionSelectors;
import au.com.dius.pact.provider.junitsupport.loader.SelectorBuilder;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.em.npa.rest.RedactionResource;
import uk.gov.hmcts.reform.em.npa.service.DeleteService;
import uk.gov.hmcts.reform.em.npa.service.MarkUpService;
import uk.gov.hmcts.reform.em.npa.service.RedactionService;

import java.io.File;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

@Provider("em_npa_redaction_api")
//@PactFolder("pacts")
@PactBroker(
    url = "${PACT_BROKER_FULL_URL:http://localhost:80}",
    providerBranch = "${pact.provider.branch}"
)
@IgnoreNoPactsToVerify
@ExtendWith(SpringExtension.class)
@WebMvcTest(value = RedactionResource.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ClientAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
public class NpaPactRedactionProviderTest {

    private final MockMvc mockMvc;

    @MockitoBean
    private RedactionService redactionService;

    @MockitoBean
    private MarkUpService markUpService;

    @MockitoBean
    private DeleteService deleteService;

    private static final File TEST_REDACTED_PDF_FILE = new File(
            ClassLoader.getSystemResource("document-redacted.pdf").getPath()
    );

    @Autowired
    public NpaPactRedactionProviderTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        if (context != null) {
            context.verifyInteraction();
        }
    }

    @BeforeEach
    void before(PactVerificationContext context) {
        System.getProperties().setProperty("pact.verifier.publishResults", "true");
        if (context != null) {
            context.setTarget(new MockMvcTestTarget(mockMvc));
        }
    }

    @PactBrokerConsumerVersionSelectors
    public static SelectorBuilder consumerVersionSelectors() {
        return new SelectorBuilder()
            .matchingBranch()
            .mainBranch()
            .deployedOrReleased();
    }

    @State("Valid redaction request exists")
    public void setupValidRedactionRequest() {

        File mockFile = TEST_REDACTED_PDF_FILE;

        when(redactionService.redactFile(
                any(String.class),
                any(String.class),
                argThat(redactionRequest ->
                    !redactionRequest.getRedactions().isEmpty() // or even just > 0
                )
        )).thenReturn(mockFile);
    }
}