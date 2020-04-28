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
import org.springframework.validation.BindingResult;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.MarkUpDTO;
import uk.gov.hmcts.reform.em.npa.rest.errors.EmptyResponseException;
import uk.gov.hmcts.reform.em.npa.service.MarkUpService;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

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
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void createMarkUpSuccess() throws URISyntaxException {

        MarkUpDTO markUpDTO = createMarkUpDTO();

        Mockito.when(markUpService.save(markUpDTO)).thenReturn(markUpDTO);

        ResponseEntity<MarkUpDTO>  responseEntity = markUpResource.createMarkUp(markUpDTO, result);

        MarkUpDTO response = responseEntity.getBody();
        Assert.assertEquals(markUpDTO.getDocumentId(), response.getDocumentId());
        Assert.assertEquals(markUpDTO.getId(), response.getId());
        Assert.assertEquals(markUpDTO.getHeight(), response.getHeight());
        Assert.assertEquals(markUpDTO.getWidth(), response.getWidth());
        Assert.assertEquals(markUpDTO.getXcoordinate(), response.getXcoordinate());
        Assert.assertEquals(markUpDTO.getYcoordinate(), response.getYcoordinate());

        Mockito.verify(markUpService, Mockito.atLeast(1)).save(markUpDTO);
    }

    @Test
    public void updateMarkUpSuccess() throws URISyntaxException {

        MarkUpDTO markUpDTO = createMarkUpDTO();

        Mockito.when(markUpService.save(markUpDTO)).thenReturn(markUpDTO);

        ResponseEntity<MarkUpDTO>  responseEntity = markUpResource.updateMarkUp(markUpDTO, result);

        MarkUpDTO response = responseEntity.getBody();
        Assert.assertEquals(markUpDTO.getDocumentId(), response.getDocumentId());
        Assert.assertEquals(markUpDTO.getId(), response.getId());
        Assert.assertEquals(markUpDTO.getHeight(), response.getHeight());
        Assert.assertEquals(markUpDTO.getWidth(), response.getWidth());
        Assert.assertEquals(markUpDTO.getXcoordinate(), response.getXcoordinate());
        Assert.assertEquals(markUpDTO.getYcoordinate(), response.getYcoordinate());

        Mockito.verify(markUpService, Mockito.atLeast(1)).save(markUpDTO);
    }

    @Test
    public void testGetAllDocumentMarkUpsSuccess() {

        UUID id =  UUID.randomUUID();
        List<MarkUpDTO> markUps = Arrays.asList(createMarkUpDTO());
        Page<MarkUpDTO> markUpDtos = new PageImpl<>(markUps);

        Mockito.when(markUpService.findAllByDocumentId(id, pageable)).thenReturn(markUpDtos);
        ResponseEntity<List<MarkUpDTO>> response = markUpResource.getAllDocumentMarkUps(id, pageable);

        Mockito.verify(markUpService, Mockito.atLeast(1)).findAllByDocumentId(id, pageable);
    }

    @Test(expected = EmptyResponseException.class)
    public void testGetAllDocumentMarkUpsFailure() {

        UUID id =  UUID.randomUUID();

        Page<MarkUpDTO> markUpDtos = new PageImpl<>(Collections.emptyList());

        Mockito.when(markUpService.findAllByDocumentId(id, pageable)).thenReturn(markUpDtos);
        markUpResource.getAllDocumentMarkUps(id, pageable);
    }

    @Test
    public void testDeleteMarkUpSuccess() {

        UUID id =  UUID.randomUUID();
        Mockito.doNothing().when(markUpService).delete(id);

        ResponseEntity<Void> response = markUpResource.deleteMarkUp(id);

        Mockito.verify(markUpService, Mockito.atLeast(1)).delete(id);
        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    private MarkUpDTO createMarkUpDTO() {
        MarkUpDTO markUpDTO = new MarkUpDTO();
        markUpDTO.setDocumentId(UUID.randomUUID());
        markUpDTO.setId(UUID.randomUUID());
        markUpDTO.setHeight(10);
        markUpDTO.setWidth(10);
        markUpDTO.setXcoordinate(20);
        markUpDTO.setYcoordinate(30);
        return markUpDTO;
    }
}
