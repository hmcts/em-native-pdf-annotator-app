package uk.gov.hmcts.reform.em.npa.rest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.WebDataBinder;
import uk.gov.hmcts.reform.em.npa.config.Constants;
import uk.gov.hmcts.reform.em.npa.rest.errors.EmptyResponseException;
import uk.gov.hmcts.reform.em.npa.rest.errors.ValidationErrorException;
import uk.gov.hmcts.reform.em.npa.service.MarkUpService;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RectangleDTO;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RedactionDTO;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RedactionSetDTO;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarkUpResourceTest {

    @InjectMocks
    private MarkUpResource markUpResource;

    @Mock
    private MarkUpService markUpService;

    @Mock
    private BindingResult result;

    @Mock
    private Pageable pageable;

    @Mock
    private WebDataBinder binder;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createMarkUpFailure() {

        RedactionDTO redactionDTO = createRedactionDTO();
        String[] codes = {"1"};
        FieldError fieldError = new FieldError(
                "testField",
                "field",
                null,
                true,
                codes,
                null,
                null
        );
        List<FieldError> errors = Arrays.asList(fieldError);
        when(result.hasErrors()).thenReturn(true);
        when(result.getFieldErrors()).thenReturn(errors);
        assertThrows(ValidationErrorException.class, () ->
            markUpResource.createMarkUp(redactionDTO, result));
    }

    @Test
    void createMarkUpSuccess() throws URISyntaxException {

        RedactionDTO redactionDTO = createRedactionDTO();

        when(markUpService.save(redactionDTO)).thenReturn(redactionDTO);

        ResponseEntity<RedactionDTO>  responseEntity = markUpResource.createMarkUp(redactionDTO, result);

        RedactionDTO response = responseEntity.getBody();
        assertEquals(redactionDTO.getDocumentId(), response.getDocumentId());
        assertEquals(redactionDTO.getRedactionId(), response.getRedactionId());
        assertEquals(redactionDTO.getRectangles().size(), response.getRectangles().size());

        verify(markUpService, atLeast(1)).save(redactionDTO);
    }

    @Test
    void createMarkUpsFailure() {
        RedactionSetDTO redactionSetDTO = createRedactionSetDTO();
        String[] codes = {"1"};
        FieldError fieldError = new FieldError("testField", "field", null, true, codes, null, null);
        List<FieldError> errors = List.of(fieldError);
        when(result.hasErrors()).thenReturn(true);
        when(result.getFieldErrors()).thenReturn(errors);

        assertThrows(ValidationErrorException.class, () ->
            markUpResource.createSearchMarkUps(redactionSetDTO, result));
    }

    @Test
    void createMarkUpsSuccess() {
        RedactionSetDTO redactionSetDTO = createRedactionSetDTO();
        when(markUpService.saveAll(any())).thenReturn(redactionSetDTO);

        ResponseEntity<RedactionSetDTO>  responseEntity = markUpResource.createSearchMarkUps(redactionSetDTO, result);
        RedactionSetDTO response = responseEntity.getBody();

        assertNotNull(response);
        assertTrue(redactionSetDTO.getSearchRedactions().contains(response.getSearchRedactions().iterator().next()));
        verify(markUpService, atLeast(1)).saveAll(redactionSetDTO);
    }

    @Test
    void updateMarkUpFailure() {

        RedactionDTO redactionDTO = createRedactionDTO();
        String[] codes = {"1"};
        FieldError fieldError = new FieldError(
                "testField",
                "field",
                null,
                true, codes,
                null,
                null
        );
        List<FieldError> errors = Arrays.asList(fieldError);
        when(result.hasErrors()).thenReturn(true);
        when(result.getFieldErrors()).thenReturn(errors);
        assertThrows(ValidationErrorException.class, () ->
            markUpResource.updateMarkUp(redactionDTO, result));

    }

    @Test
    void updateMarkUpSuccess() {

        RedactionDTO redactionDTO = createRedactionDTO();

        when(markUpService.save(redactionDTO)).thenReturn(redactionDTO);

        ResponseEntity<RedactionDTO>  responseEntity = markUpResource.updateMarkUp(redactionDTO, result);

        RedactionDTO response = responseEntity.getBody();
        assertEquals(redactionDTO.getDocumentId(), response.getDocumentId());
        assertEquals(redactionDTO.getRedactionId(), response.getRedactionId());
        assertEquals(redactionDTO.getRectangles().size(), response.getRectangles().size());

        verify(markUpService, atLeast(1)).save(redactionDTO);
    }

    @Test
    void testGetAllDocumentMarkUpsSuccess() {

        UUID id =  UUID.randomUUID();
        List<RedactionDTO> redactions = Arrays.asList(createRedactionDTO());
        Page<RedactionDTO> redactionDTOS = new PageImpl<>(redactions);

        when(markUpService.findAllByDocumentId(id, Pageable.unpaged())).thenReturn(redactionDTOS);
        markUpResource.getAllDocumentMarkUps(id, pageable);

        verify(markUpService, atLeast(1)).findAllByDocumentId(id, Pageable.unpaged());
    }

    @Test
    void testGetAllDocumentMarkUpsFailure() {

        UUID id =  UUID.randomUUID();

        Page<RedactionDTO> redactionDTOS = new PageImpl<>(Collections.emptyList());

        when(markUpService.findAllByDocumentId(id, Pageable.unpaged())).thenReturn(redactionDTOS);
        assertThrows(EmptyResponseException.class, () ->
            markUpResource.getAllDocumentMarkUps(id, pageable));
    }

    @Test
    void testDeleteMarkUpsSuccess() {

        UUID documentId =  UUID.randomUUID();
        doNothing().when(markUpService).deleteAll(documentId);

        ResponseEntity<Void> response = markUpResource.deleteMarkUps(documentId);

        verify(markUpService, atLeast(1)).deleteAll(documentId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testDeleteMarkUpSuccess() {

        UUID documentId =  UUID.randomUUID();
        UUID redactionId =  UUID.randomUUID();
        doNothing().when(markUpService).delete(redactionId);

        ResponseEntity<Void> response = markUpResource.deleteMarkUp(documentId, redactionId);

        verify(markUpService, atLeast(1)).delete(redactionId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testInitBinder() {

        WebDataBinder webDataBinder = new WebDataBinder(null);

        assertNull(webDataBinder.getDisallowedFields());
        markUpResource.initBinder(webDataBinder);
        assertTrue(Arrays.asList(webDataBinder.getDisallowedFields()).contains(Constants.IS_ADMIN));
    }

    private RedactionSetDTO createRedactionSetDTO() {
        return new RedactionSetDTO(Set.of(createRedactionDTO(), createRedactionDTO(), createRedactionDTO()));
    }

    private RedactionDTO createRedactionDTO() {
        RedactionDTO redactionDTO = new RedactionDTO();
        redactionDTO.setDocumentId(UUID.randomUUID());
        redactionDTO.setRedactionId(UUID.randomUUID());
        Set<RectangleDTO> rectangles = new HashSet<>();
        rectangles.add(createRectangleDTO());
        redactionDTO.setRectangles(rectangles);
        return redactionDTO;
    }

    private RectangleDTO createRectangleDTO() {
        RectangleDTO rectangleDTO = new RectangleDTO();
        rectangleDTO.setId(UUID.randomUUID());
        rectangleDTO.setHeight(10.0);
        rectangleDTO.setWidth(10.0);
        rectangleDTO.setX(20.0);
        rectangleDTO.setY(30.0);
        return rectangleDTO;
    }
}
