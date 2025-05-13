package uk.gov.hmcts.reform.em.npa.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RedactionSetDTO;
import uk.gov.hmcts.reform.em.npa.service.mapper.MarkUpMapper;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarkUpServiceImplTest {

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

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSaveSuccess() {

        RedactionDTO redactionDTO = createRedactionDTO();
        redactionDTO.setRectangles(Collections.emptySet());
        Redaction redaction = createRedaction();
        redaction.setRectangles(Collections.emptySet());
        when(securityUtils.getCurrentUserLogin()).thenReturn(Optional.of("testuser"));
        when(markUpMapper.toEntity(redactionDTO)).thenReturn(redaction);
        when(markUpMapper.toDto(redaction)).thenReturn(redactionDTO);
        when(markUpRepository.save(redaction)).thenReturn(redaction);

        RedactionDTO updatedDto = markUpService.save(redactionDTO);

        assertEquals(redactionDTO.getDocumentId(), updatedDto.getDocumentId());
        assertEquals(redactionDTO.getRedactionId(), updatedDto.getRedactionId());
        assertEquals(redactionDTO.getRectangles().size(), updatedDto.getRectangles().size());

        verify(markUpRepository, Mockito.atLeast(1)).save(redaction);
        verify(markUpMapper, Mockito.atLeast(1)).toEntity(redactionDTO);
        verify(markUpMapper, Mockito.atLeast(1)).toDto(redaction);
    }

    @Test
    void testSaveSuccessWithRectangles() {

        RedactionDTO redactionDTO = createRedactionDTO();
        Redaction redaction = createRedaction();
        when(securityUtils.getCurrentUserLogin()).thenReturn(Optional.of("testuser"));
        when(markUpMapper.toEntity(redactionDTO)).thenReturn(redaction);
        when(markUpMapper.toDto(redaction)).thenReturn(redactionDTO);
        when(markUpRepository.save(redaction)).thenReturn(redaction);

        RedactionDTO updatedDto = markUpService.save(redactionDTO);

        assertEquals(redactionDTO.getDocumentId(), updatedDto.getDocumentId());
        assertEquals(redactionDTO.getRedactionId(), updatedDto.getRedactionId());
        assertEquals(redactionDTO.getRectangles().size(), updatedDto.getRectangles().size());

        verify(markUpRepository, Mockito.atLeast(1)).save(redaction);
        verify(markUpMapper, Mockito.atLeast(1)).toEntity(redactionDTO);
        verify(markUpMapper, Mockito.atLeast(1)).toDto(redaction);
    }

    @Test
    void testSaveFailure() {
        RedactionDTO redactionDTO = createRedactionDTO();
        assertThrows(UsernameNotFoundException.class, () -> markUpService.save(redactionDTO));
    }

    @Test
    void testSaveAllSuccess() {

        RedactionDTO redactionDTO = createRedactionDTO();
        RedactionDTO redactionDTO1 = createRedactionDTO();
        RedactionDTO redactionDTO2 = createRedactionDTO();
        redactionDTO2.setRectangles(Collections.emptySet());

        Redaction redaction = createRedaction();
        Redaction redaction1 = createRedaction();
        Redaction redaction2 = createRedaction();
        redaction2.setRectangles(Collections.emptySet());

        Set<RedactionDTO> redactionDTOS = Set.of(redactionDTO, redactionDTO1, redactionDTO2);
        RedactionSetDTO redactionSetDTO = new RedactionSetDTO(redactionDTOS);
        List<Redaction> redactionList = List.of(redaction, redaction1, redaction2);
        Set<Redaction> redactionSet = Set.copyOf(redactionList);
        when(securityUtils.getCurrentUserLogin()).thenReturn(Optional.of("testuser"));
        when(markUpRepository.saveAll(Mockito.any())).thenReturn(redactionList);
        when(markUpMapper.toEntity(redactionSetDTO.getSearchRedactions())).thenReturn(redactionSet);
        when(markUpMapper.toDto(redactionSet)).thenReturn(redactionDTOS);


        RedactionSetDTO updatedDto = markUpService.saveAll(redactionSetDTO);

        Set<RedactionDTO> updatedRedactionDTOS = updatedDto.getSearchRedactions();
        assertEquals(3, updatedRedactionDTOS.size());

        RedactionDTO savedRedactionDTO = updatedRedactionDTOS.iterator().next();

        assertTrue(savedRedactionDTO.equals(redactionDTO)
                || savedRedactionDTO.equals(redactionDTO1) || savedRedactionDTO.equals(redactionDTO2));

        verify(markUpRepository, Mockito.atLeast(1)).saveAll(Mockito.any());
        verify(markUpMapper, Mockito.atLeast(1)).toEntity(redactionDTOS);
        verify(markUpMapper, Mockito.atLeast(1)).toDto(redactionSet);
    }

    @Test
    void testSaveAllFailure() {

        RedactionDTO redactionDTO = createRedactionDTO();
        RedactionDTO redactionDTO1 = createRedactionDTO();
        RedactionDTO redactionDTO2 = createRedactionDTO();

        Redaction redaction = createRedaction();
        Redaction redaction1 = createRedaction();
        Redaction redaction2 = createRedaction();

        Set<RedactionDTO> redactionDTOS = Set.of(redactionDTO, redactionDTO1, redactionDTO2);
        RedactionSetDTO redactionSetDTO = new RedactionSetDTO(redactionDTOS);
        Set<Redaction> redactionSet = Set.of(redaction, redaction1, redaction2);
        when(markUpMapper.toEntity(redactionSetDTO.getSearchRedactions())).thenReturn(redactionSet);

        assertThrows(UsernameNotFoundException.class, () ->
            markUpService.saveAll(redactionSetDTO));
    }


    @Test
    void testFindAllByDocumentIdSuccess() {

        RedactionDTO redactionDTO = createRedactionDTO();
        Redaction redaction = createRedaction();
        List<Redaction> markUps = Arrays.asList(redaction);
        Page<Redaction> pagedResponse = new PageImpl<>(markUps);
        UUID id =  UUID.randomUUID();

        when(securityUtils.getCurrentUserLogin()).thenReturn(Optional.of("testuser"));
        when(markUpRepository.findByDocumentIdAndCreatedBy(id, "testuser", pageable))
            .thenReturn(pagedResponse);
        when(markUpMapper.toDto(redaction)).thenReturn(redactionDTO);

        markUpService.findAllByDocumentId(id, pageable);

        verify(markUpRepository, Mockito.atLeast(1))
            .findByDocumentIdAndCreatedBy(id, "testuser", pageable);
    }

    @Test
    void testDeleteSuccess() {
        UUID id =  UUID.randomUUID();

        markUpService.delete(id);

        doNothing().when(markUpRepository).deleteByRedactionId(id);
        verify(markUpRepository, Mockito.atLeast(1)).deleteByRedactionId(id);
    }

    @Test
    void testDeleteAllSuccess() {
        UUID documentId =  UUID.randomUUID();
        when(securityUtils.getCurrentUserLogin()).thenReturn(Optional.of("testuser"));

        markUpService.deleteAll(documentId);

        doNothing().when(markUpRepository).deleteAllByDocumentIdAndCreatedBy(documentId, "testuser");
        verify(markUpRepository, Mockito.atLeast(1)).deleteAllByDocumentIdAndCreatedBy(documentId, "testuser");
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
