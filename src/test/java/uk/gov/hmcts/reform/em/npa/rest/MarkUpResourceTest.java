package uk.gov.hmcts.reform.em.npa.rest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
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

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MarkUpResourceTest {

    @InjectMocks
    private MarkUpResource markUpResource;

    @Mock
    private MarkUpService markUpService;

    @Mock
    private BindingResult result;

    @Mock
    private Pageable pageable;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test(expected = ValidationErrorException.class)
    public void createMarkUpFailure() throws URISyntaxException {

        RedactionDTO redactionDTO = createRedactionDTO();
        String[] codes = {"1"};
        FieldError fieldError = new FieldError("testField", "field", null, true, codes, null, null);
        List<FieldError> errors = List.of(fieldError);
        when(result.hasErrors()).thenReturn(true);
        when(result.getFieldErrors()).thenReturn(errors);

        markUpResource.createMarkUp(redactionDTO, result);
    }

    @Test
    public void createMarkUpSuccess() throws URISyntaxException {

        RedactionDTO redactionDto = createRedactionDTO();

        when(markUpService.save(redactionDto)).thenReturn(redactionDto);

        ResponseEntity<RedactionDTO>  responseEntity = markUpResource.createMarkUp(redactionDto, result);

        RedactionDTO response = responseEntity.getBody();
        assert response != null;
        assertEquals(redactionDto.getDocumentId(), response.getDocumentId());
        assertEquals(redactionDto.getRedactionId(), response.getRedactionId());
        assertEquals(redactionDto.getRectangles().size(), response.getRectangles().size());

        verify(markUpService, atLeast(1)).save(redactionDto);
    }

    @Test(expected = ValidationErrorException.class)
    public void updateMarkUpFailure() {
        RedactionDTO redactionDto = createRedactionDTO();
        String[] codes = {"1"};
        FieldError fieldError = new FieldError("testField", "field", null, true, codes, null, null);
        List<FieldError> errors = List.of(fieldError);
        when(result.hasErrors()).thenReturn(true);
        when(result.getFieldErrors()).thenReturn(errors);

        markUpResource.updateMarkUp(redactionDto, result);

    }

    @Test
    public void updateMarkUpSuccess() {

        RedactionDTO redactionDto = createRedactionDTO();

        when(markUpService.save(redactionDto)).thenReturn(redactionDto);

        ResponseEntity<RedactionDTO>  responseEntity = markUpResource.updateMarkUp(redactionDto, result);

        RedactionDTO response = responseEntity.getBody();
        assert response != null;
        assertEquals(redactionDto.getDocumentId(), response.getDocumentId());
        assertEquals(redactionDto.getRedactionId(), response.getRedactionId());
        assertEquals(redactionDto.getRectangles().size(), response.getRectangles().size());

        verify(markUpService, atLeast(1)).save(redactionDto);
    }

    @Test
    public void testGetAllDocumentMarkUpsSuccess() {

        UUID id =  UUID.randomUUID();
        List<RedactionDTO> redactions = List.of(createRedactionDTO());
        Page<RedactionDTO> redactionDtos = new PageImpl<>(redactions);

        when(markUpService.findAllByDocumentId(id, pageable)).thenReturn(redactionDtos);
        markUpResource.getAllDocumentMarkUps(id, pageable);
        verify(markUpService, atLeast(1)).findAllByDocumentId(id, pageable);
    }

    @Test(expected = EmptyResponseException.class)
    public void testGetAllDocumentMarkUpsFailure() {

        UUID id =  UUID.randomUUID();

        Page<RedactionDTO> redactionDtos = new PageImpl<>(Collections.emptyList());

        when(markUpService.findAllByDocumentId(id, pageable)).thenReturn(redactionDtos);
        markUpResource.getAllDocumentMarkUps(id, pageable);
    }

    @Test
    public void testDeleteMarkUpsSuccess() {

        UUID documentId =  UUID.randomUUID();
        doNothing().when(markUpService).deleteAll(documentId);

        ResponseEntity<Void> response = markUpResource.deleteMarkUps(documentId);

        verify(markUpService, atLeast(1)).deleteAll(documentId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void testDeleteMarkUpSuccess() {

        UUID documentId =  UUID.randomUUID();
        UUID redactionId =  UUID.randomUUID();
        doNothing().when(markUpService).delete(redactionId);

        ResponseEntity<Void> response = markUpResource.deleteMarkUp(documentId, redactionId);

        verify(markUpService, atLeast(1)).delete(redactionId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void testInitBinder() {

        WebDataBinder webDataBinder = new WebDataBinder(null);

        Assert.assertNull(webDataBinder.getDisallowedFields());
        markUpResource.initBinder(webDataBinder);
        Assert.assertTrue(Arrays.asList(webDataBinder.getDisallowedFields()).contains(Constants.IS_ADMIN));
    }

    private RedactionDTO createRedactionDTO() {
        RedactionDTO redactionDto = new RedactionDTO();
        redactionDto.setDocumentId(UUID.randomUUID());
        redactionDto.setRedactionId(UUID.randomUUID());
        Set<RectangleDTO> rectangles = new HashSet<>();
        rectangles.add(createRectangleDTO());
        redactionDto.setRectangles(rectangles);
        return redactionDto;
    }

    private RectangleDTO createRectangleDTO() {
        RectangleDTO rectangleDto = new RectangleDTO();
        rectangleDto.setId(UUID.randomUUID());
        rectangleDto.setHeight(10.0);
        rectangleDto.setWidth(10.0);
        rectangleDto.setX(20.0);
        rectangleDto.setY(30.0);
        return rectangleDto;
    }
}
