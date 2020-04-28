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
import uk.gov.hmcts.reform.em.npa.ccd.exception.CaseDocumentNotFoundException;

import uk.gov.hmcts.reform.em.npa.service.RedactionService;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.MarkUpDTO;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RedactionRequest;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.UUID;

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

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    public static RedactionRequest createRequest() {
        RedactionRequest redactionRequest = new RedactionRequest();
        UUID docId = UUID.randomUUID();
        redactionRequest.setCaseId("caseId");
        redactionRequest.setDocumentId(docId);

        MarkUpDTO markUpDTO = new MarkUpDTO();
        markUpDTO.setId(UUID.randomUUID());
        markUpDTO.setDocumentId(docId);
        markUpDTO.setPageNumber(1);
        markUpDTO.setHeight(100);
        markUpDTO.setWidth(100);
        markUpDTO.setXcoordinate(100);
        markUpDTO.setYcoordinate(100);

        redactionRequest.setMarkups(Arrays.asList(markUpDTO));

        return redactionRequest;
    }

    @Test
    public void shouldSaveRedactedDocument() {
        RedactionRequest redactionRequest = createRequest();
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.getHeader("Authorization")).thenReturn("jwt");
        when(redactionService.redactFile("jwt", redactionRequest.getCaseId(), redactionRequest.getDocumentId(), redactionRequest.getMarkups()))
                .thenReturn("newDocId");

        ResponseEntity<String> response = redactionResource.save(request, redactionRequest);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals("newDocId", response.getBody());

        verify(redactionService, Mockito.atMost(1))
                .redactFile("jwt", redactionRequest.getCaseId(), redactionRequest.getDocumentId(), redactionRequest.getMarkups());
    }

    @Test
    public void shouldFailSavingRedactedDocument() {
        RedactionRequest redactionRequest = createRequest();
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.getHeader("Authorization")).thenReturn("jwt");
        when(redactionService.redactFile("jwt", redactionRequest.getCaseId(), redactionRequest.getDocumentId(), redactionRequest.getMarkups()))
                .thenThrow(CaseDocumentNotFoundException.class);

        ResponseEntity<String> response = redactionResource.save(request, redactionRequest);
        assertEquals(400, response.getStatusCodeValue());

        verify(redactionService, Mockito.atMost(1))
                .redactFile("jwt", redactionRequest.getCaseId(), redactionRequest.getDocumentId(), redactionRequest.getMarkups());
    }
}
