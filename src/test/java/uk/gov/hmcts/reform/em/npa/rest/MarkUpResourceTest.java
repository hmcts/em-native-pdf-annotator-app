package uk.gov.hmcts.reform.em.npa.rest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
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
import java.util.*;

public class MarkUpResourceTest {

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

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test(expected = ValidationErrorException.class)
    public void createMarkUpFailure() throws URISyntaxException {

        RedactionDTO redactionDTO = createRedactionDTO();
        String[] codes = {"1"};
        FieldError fieldError = new FieldError("testField", "field", null, true, codes, null, null);
        List<FieldError> errors = Arrays.asList(fieldError);
        Mockito.when(result.hasErrors()).thenReturn(true);
        Mockito.when(result.getFieldErrors()).thenReturn(errors);

        ResponseEntity<RedactionDTO>  responseEntity = markUpResource.createMarkUp(redactionDTO, result);
    }

    @Test
    public void createMarkUpSuccess() throws URISyntaxException {

        RedactionDTO redactionDTO = createRedactionDTO();

        Mockito.when(markUpService.save(redactionDTO)).thenReturn(redactionDTO);

        ResponseEntity<RedactionDTO>  responseEntity = markUpResource.createMarkUp(redactionDTO, result);

        RedactionDTO response = responseEntity.getBody();
        Assert.assertEquals(redactionDTO.getDocumentId(), response.getDocumentId());
        Assert.assertEquals(redactionDTO.getRedactionId(), response.getRedactionId());
        Assert.assertEquals(redactionDTO.getRectangles().size(), response.getRectangles().size());

        Mockito.verify(markUpService, Mockito.atLeast(1)).save(redactionDTO);
    }

    @Test(expected = ValidationErrorException.class)
    public void updateMarkUpFailure() throws URISyntaxException {

        RedactionDTO redactionDTO = createRedactionDTO();
        String[] codes = {"1"};
        FieldError fieldError = new FieldError("testField", "field", null, true, codes, null, null);
        List<FieldError> errors = Arrays.asList(fieldError);
        Mockito.when(result.hasErrors()).thenReturn(true);
        Mockito.when(result.getFieldErrors()).thenReturn(errors);

        ResponseEntity<RedactionDTO>  responseEntity = markUpResource.updateMarkUp(redactionDTO, result);

    }

    @Test
    public void updateMarkUpSuccess() throws URISyntaxException {

        RedactionDTO redactionDTO = createRedactionDTO();

        Mockito.when(markUpService.save(redactionDTO)).thenReturn(redactionDTO);

        ResponseEntity<RedactionDTO>  responseEntity = markUpResource.updateMarkUp(redactionDTO, result);

        RedactionDTO response = responseEntity.getBody();
        Assert.assertEquals(redactionDTO.getDocumentId(), response.getDocumentId());
        Assert.assertEquals(redactionDTO.getRedactionId(), response.getRedactionId());
        Assert.assertEquals(redactionDTO.getRectangles().size(), response.getRectangles().size());

        Mockito.verify(markUpService, Mockito.atLeast(1)).save(redactionDTO);
    }

    @Test
    public void testGetAllDocumentMarkUpsSuccess() {

        UUID id =  UUID.randomUUID();
        List<RedactionDTO> redactions = Arrays.asList(createRedactionDTO());
        Page<RedactionDTO> redactionDTOS = new PageImpl<>(redactions);

        Mockito.when(markUpService.findAllByDocumentId(id, pageable)).thenReturn(redactionDTOS);
        ResponseEntity<List<RedactionDTO>> response = markUpResource.getAllDocumentMarkUps(id, pageable);

        Mockito.verify(markUpService, Mockito.atLeast(1)).findAllByDocumentId(id, pageable);
    }

    @Test(expected = EmptyResponseException.class)
    public void testGetAllDocumentMarkUpsFailure() {

        UUID id =  UUID.randomUUID();

        Page<RedactionDTO> redactionDTOS = new PageImpl<>(Collections.emptyList());

        Mockito.when(markUpService.findAllByDocumentId(id, pageable)).thenReturn(redactionDTOS);
        markUpResource.getAllDocumentMarkUps(id, pageable);
    }

    @Test
    public void testDeleteMarkUpsSuccess() {

        UUID documentId =  UUID.randomUUID();
        Mockito.doNothing().when(markUpService).deleteAll(documentId);

        ResponseEntity<Void> response = markUpResource.deleteMarkUps(documentId);

        Mockito.verify(markUpService, Mockito.atLeast(1)).deleteAll(documentId);
        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void testDeleteMarkUpSuccess() {

        UUID documentId =  UUID.randomUUID();
        UUID redactionId =  UUID.randomUUID();
        Mockito.doNothing().when(markUpService).delete(redactionId);

        ResponseEntity<Void> response = markUpResource.deleteMarkUp(documentId, redactionId);

        Mockito.verify(markUpService, Mockito.atLeast(1)).delete(redactionId);
        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void testInitBinder() {

        WebDataBinder webDataBinder = new WebDataBinder(null);

        Assert.assertNull(webDataBinder.getDisallowedFields());
        markUpResource.initBinder(webDataBinder);
        Assert.assertTrue(Arrays.asList(webDataBinder.getDisallowedFields()).contains(Constants.IS_ADMIN));
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
