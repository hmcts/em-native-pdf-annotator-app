

package uk.gov.hmcts.reform.em.npa.provider;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.VersionSelector;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.em.npa.rest.MarkUpResource;
import uk.gov.hmcts.reform.em.npa.service.MarkUpService;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RectangleDTO;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RedactionDTO;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;


@Provider("native_pdf_annotator_api_provider")
//Uncomment @PactFolder and comment the @PactBroker line to test local consumer.
//using this, import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
//@PactFolder("target/pacts")
@PactBroker(scheme = "${PACT_BROKER_SCHEME:http}",
        host = "${PACT_BROKER_URL:localhost}",
        port = "${PACT_BROKER_PORT:80}",
        consumerVersionSelectors = {@VersionSelector(tag = "master")})
@IgnoreNoPactsToVerify
@ExtendWith(SpringExtension.class)
@WebMvcTest(value = MarkUpResource.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ClientAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
public class NpaPactProviderTest {


    private static final UUID REDACTION_ID = UUID.fromString("4c34ba4a-585a-407d-aa78-3f86f3171cdd");
    private static final UUID DOCUMENT_ID = UUID.fromString("f2cc4d79-d0f3-4b43-affe-535516370cdd");
    private static final UUID RECTANGLE_ID = UUID.fromString("c04b807f-8352-4bfc-95b5-cecd072b7aba");

    // Static coordinates
    private static final Double X = 10.5;
    private static final Double Y = 20.5;
    private static final Double WIDTH = 100.0;
    private static final Double HEIGHT = 200.0;
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MarkUpService markUpService;

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

    @State("A valid RedactionDTO exists")
    public void setupValidRedactionDTO() {
        when(markUpService.save(any(RedactionDTO.class))).thenAnswer(invocation -> {
            RedactionDTO request = invocation.getArgument(0); // Get the request object

            RectangleDTO rectangle = new RectangleDTO();
            rectangle.setId(request.getRectangles().iterator().next().getId()); // Use request's rectangle ID
            rectangle.setX(X);
            rectangle.setY(Y);
            rectangle.setWidth(WIDTH);
            rectangle.setHeight(HEIGHT);

            RedactionDTO response = new RedactionDTO();
            response.setRedactionId(request.getRedactionId()); // Echo request's redaction ID
            response.setDocumentId(request.getDocumentId());   // Echo request's document ID
            response.setPage(1);
            response.setRectangles(Set.of(rectangle));

            return response;
        });
    }

    @State("Markups exist for document f2cc4d79-d0f3-4b43-affe-535516370cdd")
    public void setupMarkupsForDocument() {
        RectangleDTO rectangle = new RectangleDTO();
        rectangle.setId(RECTANGLE_ID);
        rectangle.setX(X);
        rectangle.setY(Y);
        rectangle.setWidth(WIDTH);
        rectangle.setHeight(HEIGHT);

        RedactionDTO markup = new RedactionDTO();
        markup.setRedactionId(REDACTION_ID);
        markup.setDocumentId(DOCUMENT_ID); // Must match the ID in your pact
        markup.setPage(1);
        markup.setRectangles(Set.of(rectangle));

        when(markUpService.findAllByDocumentId(eq(DOCUMENT_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(markup)));
    }


    @State("Markups exist for document ID and can be deleted")
    public void setupDeleteMarkupsForDocument() {
        doNothing().when(markUpService).deleteAll(DOCUMENT_ID);
    }


    @State("A valid Redaction with document ID and redaction ID exists and can be deleted")
    public void setupDeleteSingleMarkup() {
        doNothing().when(markUpService).delete(REDACTION_ID);
    }

    @State("A valid RedactionDTO with ID exists")
    public void setupExistingRedactionForUpdate() {
        when(markUpService.save(any(RedactionDTO.class))).thenAnswer(invocation -> {
            RedactionDTO request = invocation.getArgument(0);

            // Verify the incoming ID matches our expected UUID
            if (!REDACTION_ID.equals(request.getRedactionId())) {
                throw new RuntimeException("Unexpected redaction ID");
            }

            RectangleDTO rectangle = new RectangleDTO();
            rectangle.setId(request.getRectangles().iterator().next().getId());
            rectangle.setX(X);
            rectangle.setY(Y);
            rectangle.setWidth(WIDTH);
            rectangle.setHeight(HEIGHT);

            RedactionDTO response = new RedactionDTO();
            response.setRedactionId(request.getRedactionId());
            response.setDocumentId(request.getDocumentId());
            response.setPage(1);
            response.setRectangles(Set.of(rectangle));

            return response;
        });
    }
}