package uk.gov.hmcts.reform.em.npa.service.impl;

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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import uk.gov.hmcts.reform.em.npa.config.security.SecurityUtils;
import uk.gov.hmcts.reform.em.npa.domain.Rectangle;
import uk.gov.hmcts.reform.em.npa.domain.Redaction;
import uk.gov.hmcts.reform.em.npa.repository.MarkUpRepository;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RectangleDTO;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RedactionDTO;
import uk.gov.hmcts.reform.em.npa.service.mapper.MarkUpMapper;

import java.util.*;

public class MarkUpServiceImplTest {

    @InjectMocks
    private MarkUpServiceImpl markUpService;

    @Mock
    private MarkUpRepository markUpRepository;

    @Mock
    private MarkUpMapper markUpMapper;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private Pageable pageable;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testSaveSuccess() {

        RedactionDTO redactionDTO = createRedactionDTO();
        Redaction redaction = createRedaction();
        Mockito.when(securityUtils.getCurrentUserLogin()).thenReturn(Optional.of("testuser"));
        Mockito.when(markUpMapper.toEntity(redactionDTO)).thenReturn(redaction);
        Mockito.when(markUpMapper.toDto(redaction)).thenReturn(redactionDTO);
        Mockito.when(markUpRepository.save(redaction)).thenReturn(redaction);

        RedactionDTO updatedDto = markUpService.save(redactionDTO);

        Mockito.verify(markUpRepository, Mockito.atLeast(1)).save(redaction);
        Mockito.verify(markUpMapper, Mockito.atLeast(1)).toEntity(redactionDTO);
        Mockito.verify(markUpMapper, Mockito.atLeast(1)).toDto(redaction);

        Assert.assertEquals(redactionDTO.getDocumentId(), updatedDto.getDocumentId());
        Assert.assertEquals(redactionDTO.getRedactionId(), updatedDto.getRedactionId());
        Assert.assertEquals(redactionDTO.getRectangles().size(), updatedDto.getRectangles().size());

    }

    @Test(expected = UsernameNotFoundException.class)
    public void testSaveFailure() {

        RedactionDTO redactionDTO = createRedactionDTO();

        RedactionDTO updatedDto = markUpService.save(redactionDTO);
    }

    @Test
    public void testFindAllByDocumentIdSuccess() {

        RedactionDTO redactionDTO = createRedactionDTO();
        Redaction redaction = createRedaction();
        List<Redaction> markUps = Arrays.asList(redaction);
        Page<Redaction> pagedResponse = new PageImpl<>(markUps);
        UUID id =  UUID.randomUUID();

        Mockito.when(securityUtils.getCurrentUserLogin()).thenReturn(Optional.of("testuser"));
        Mockito.when(markUpRepository.findByDocumentIdAndCreatedBy(id, "testuser", pageable))
            .thenReturn(pagedResponse);
        Mockito.when(markUpMapper.toDto(redaction)).thenReturn(redactionDTO);

        markUpService.findAllByDocumentId(id, pageable);

        Mockito.verify(markUpRepository, Mockito.atLeast(1))
            .findByDocumentIdAndCreatedBy(id, "testuser", pageable);
    }

    @Test(expected = UsernameNotFoundException.class)
    public void testFindAllByDocumentIdFailure() {

        markUpService.findAllByDocumentId(UUID.randomUUID(), pageable);

    }

    @Test
    public void testDeleteSuccess() {
        UUID id =  UUID.randomUUID();

        markUpService.delete(id);

        Mockito.doNothing().when(markUpRepository).deleteByRedactionId(id);
        Mockito.verify(markUpRepository, Mockito.atLeast(1)).deleteByRedactionId(id);
    }

    @Test
    public void testDeleteAllSuccess() {
        UUID documentId =  UUID.randomUUID();
        Mockito.when(securityUtils.getCurrentUserLogin()).thenReturn(Optional.of("testuser"));

        markUpService.deleteAll(documentId);

        Mockito.doNothing().when(markUpRepository).deleteAllByDocumentIdAndCreatedBy(documentId, "testuser");
        Mockito.verify(markUpRepository, Mockito.atLeast(1)).deleteAllByDocumentIdAndCreatedBy(documentId, "testuser");
    }

    private Redaction createRedaction() {
        Redaction redaction = new Redaction();
        redaction.setDocumentId(UUID.randomUUID());
        redaction.setRedactionId(UUID.randomUUID());
        Set<Rectangle> rectangles = new HashSet<>();
        rectangles.add(createRectangle());
        redaction.setRectangles(rectangles);
        return redaction;
    }

    private Rectangle createRectangle() {
        Rectangle rectangle = new Rectangle();
        rectangle.setRectangleId(UUID.randomUUID());
        rectangle.setHeight(10.0);
        rectangle.setWidth(10.0);
        rectangle.setX(20.0);
        rectangle.setY(30.0);
        return rectangle;
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
