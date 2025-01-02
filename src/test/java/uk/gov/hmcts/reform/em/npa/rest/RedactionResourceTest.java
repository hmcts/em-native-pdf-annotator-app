package uk.gov.hmcts.reform.em.npa.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.bind.WebDataBinder;
import uk.gov.hmcts.reform.em.npa.Application;
import uk.gov.hmcts.reform.em.npa.TestSecurityConfiguration;
import uk.gov.hmcts.reform.em.npa.config.Constants;
import uk.gov.hmcts.reform.em.npa.service.RedactionService;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RectangleDTO;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RedactionDTO;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RedactionRequest;
import uk.gov.hmcts.reform.em.npa.service.exception.FileTypeException;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {Application.class, TestSecurityConfiguration.class})
@AutoConfigureMockMvc
public class RedactionResourceTest {

    @InjectMocks
    private RedactionResource redactionResource;

    @Mock
    private RedactionService redactionService;

    @Mock
    private WebDataBinder binder;

    private static final File TEST_PDF_FILE = new File(
            ClassLoader.getSystemResource("layered.pdf").getPath()
    );

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    public static RedactionRequest createRequest() {
        RedactionRequest redactionRequest = new RedactionRequest();
        UUID docId = UUID.randomUUID();
        List<RedactionDTO> redactions = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            RedactionDTO redaction = new RedactionDTO();
            redaction.setRedactionId(UUID.randomUUID());
            redaction.setDocumentId(docId);
            redaction.setPage(i);

            RectangleDTO rectangle = new RectangleDTO();
            rectangle.setId(UUID.randomUUID());
            rectangle.setX(100.00);
            rectangle.setY(100.00);
            rectangle.setHeight(100.00);
            rectangle.setWidth(100.00);

            redaction.setRectangles(new HashSet<>(Collections.singletonList(rectangle)));

            redactions.add(redaction);
        }

        redactionRequest.setCaseId("caseId");
        redactionRequest.setDocumentId(docId);
        redactionRequest.setRedactedFileName(null);
        redactionRequest.setRedactions(redactions);

        return redactionRequest;
    }

    @Test
    void shouldSaveRedactedDocument() {
        RedactionRequest redactionRequest = createRequest();
        redactionRequest.setRedactedFileName("bespoke");
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.getHeader("Authorization")).thenReturn("jwt");
        when(request.getHeader("ServiceAuthorization")).thenReturn("s2sToken");
        when(redactionService.redactFile("jwt", "s2sToken",redactionRequest))
                .thenReturn(TEST_PDF_FILE);

        ResponseEntity response = redactionResource.save(request, redactionRequest);
        assertEquals(200, response.getStatusCode().value());

        verify(redactionService, Mockito.atMost(1))
                .redactFile("jwt", "s2sToken",redactionRequest);
        verify(binder, Mockito.atMost(1))
            .setDisallowedFields(Constants.IS_ADMIN);
    }

    @Test
    void shouldFailSavingRedactedDocument() {
        RedactionRequest redactionRequest = createRequest();
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.getHeader("Authorization")).thenReturn("jwt");
        when(redactionService.redactFile("jwt", "s2sToken",redactionRequest))
                .thenThrow(FileTypeException.class);

        ResponseEntity response = redactionResource.save(request, redactionRequest);
        assertEquals(400, response.getStatusCode().value());

        verify(redactionService, Mockito.atMost(1))
                .redactFile("jwt", "s2sToken",redactionRequest);
    }

    @Test
    void testInitBinder() {

        WebDataBinder webDataBinder = new WebDataBinder(null);

        assertNull(webDataBinder.getDisallowedFields());
        redactionResource.initBinder(webDataBinder);
        assertTrue(Arrays.asList(webDataBinder.getDisallowedFields()).contains(Constants.IS_ADMIN));
    }
}
