package uk.gov.hmcts.reform.em.npa.rest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.em.npa.Application;
import uk.gov.hmcts.reform.em.npa.TestSecurityConfiguration;

import uk.gov.hmcts.reform.em.npa.service.RedactionService;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RectangleDTO;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RedactionDTO;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RedactionRequest;
import uk.gov.hmcts.reform.em.npa.service.exception.FileTypeException;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Application.class, TestSecurityConfiguration.class})
@AutoConfigureMockMvc
public class RedactionResourceTest {

    @InjectMocks
    private RedactionResource redactionResource;

    @Mock
    private RedactionService redactionService;

    private static final File TEST_PDF_FILE = new File(
            ClassLoader.getSystemResource("layered.pdf").getPath()
    );

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    public static RedactionRequest createRequest() {
        RedactionRequest redactionRequest = new RedactionRequest();
        UUID docId = UUID.randomUUID();
        List<RedactionDTO> redactions = new ArrayList<>();

        for (int i = 0; i < 5 ; i++) {
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
    public void shouldSaveRedactedDocument() {
        RedactionRequest redactionRequest = createRequest();
        redactionRequest.setRedactedFileName("bespoke");
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.getHeader("Authorization")).thenReturn("jwt");
        when(redactionService.redactFile("jwt", redactionRequest.getCaseId(), redactionRequest.getDocumentId(), redactionRequest.getRedactions()))
                .thenReturn(TEST_PDF_FILE);

        ResponseEntity response = redactionResource.save(request, redactionRequest);
        assertEquals(200, response.getStatusCodeValue());

        verify(redactionService, Mockito.atMost(1))
                .redactFile("jwt", redactionRequest.getCaseId(), redactionRequest.getDocumentId(), redactionRequest.getRedactions());
    }

    @Test
    public void shouldFailSavingRedactedDocument() {
        RedactionRequest redactionRequest = createRequest();
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.getHeader("Authorization")).thenReturn("jwt");
        when(redactionService.redactFile("jwt", redactionRequest.getCaseId(), redactionRequest.getDocumentId(), redactionRequest.getRedactions()))
                .thenThrow(FileTypeException.class);

        ResponseEntity response = redactionResource.save(request, redactionRequest);
        assertEquals(400, response.getStatusCodeValue());

        verify(redactionService, Mockito.atMost(1))
                .redactFile("jwt", redactionRequest.getCaseId(), redactionRequest.getDocumentId(), redactionRequest.getRedactions());
    }
}
