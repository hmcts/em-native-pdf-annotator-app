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
import uk.gov.hmcts.reform.em.npa.domain.MarkUp;
import uk.gov.hmcts.reform.em.npa.domain.MarkUpDTO;
import uk.gov.hmcts.reform.em.npa.repository.MarkUpRepository;
import uk.gov.hmcts.reform.em.npa.service.mapper.MarkUpMapper;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

        MarkUpDTO markUpDTO = createMarkUpDTO();
        MarkUp markUp = createMarkUp();
        Mockito.when(securityUtils.getCurrentUserLogin()).thenReturn(Optional.of("testuser"));
        Mockito.when(markUpMapper.toEntity(markUpDTO)).thenReturn(markUp);
        Mockito.when(markUpMapper.toDto(markUp)).thenReturn(markUpDTO);
        Mockito.when(markUpRepository.save(markUp)).thenReturn(markUp);

        MarkUpDTO updatedDto = markUpService.save(markUpDTO);

        Mockito.verify(markUpRepository, Mockito.atLeast(1)).save(markUp);
        Mockito.verify(markUpMapper, Mockito.atLeast(1)).toEntity(markUpDTO);
        Mockito.verify(markUpMapper, Mockito.atLeast(1)).toDto(markUp);

        Assert.assertEquals(markUpDTO.getDocumentId(), updatedDto.getDocumentId());
        Assert.assertEquals(markUpDTO.getId(), updatedDto.getId());
        Assert.assertEquals(markUpDTO.getHeight(), updatedDto.getHeight());
        Assert.assertEquals(markUpDTO.getWidth(), updatedDto.getWidth());
        Assert.assertEquals(markUpDTO.getXCoordinate(), updatedDto.getXCoordinate());
        Assert.assertEquals(markUpDTO.getYCoordinate(), updatedDto.getYCoordinate());

    }

    @Test(expected = UsernameNotFoundException.class)
    public void testSaveFailure() {

        MarkUpDTO markUpDTO = createMarkUpDTO();
        MarkUp markUp = createMarkUp();

        MarkUpDTO updatedDto = markUpService.save(markUpDTO);
    }

    @Test
    public void testFindAllByDocumentIdSuccess() {

        MarkUpDTO markUpDTO = createMarkUpDTO();
        MarkUp markUp = createMarkUp();
        List<MarkUp> markUps = Arrays.asList(markUp);
        Page<MarkUp> pagedResponse = new PageImpl<>(markUps);
        UUID id =  UUID.randomUUID();

        Mockito.when(securityUtils.getCurrentUserLogin()).thenReturn(Optional.of("testuser"));
        Mockito.when(markUpRepository.findByDocumentIdAndCreatedBy(id, "testuser", pageable))
            .thenReturn(pagedResponse);
        Mockito.when(markUpMapper.toDto(markUp)).thenReturn(markUpDTO);

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

        Mockito.doNothing().when(markUpRepository).deleteById(id);
        Mockito.verify(markUpRepository, Mockito.atLeast(1)).deleteById(id);
    }

    private MarkUp createMarkUp() {
        MarkUp markUp = new MarkUp();
        markUp.setDocumentId(UUID.randomUUID());
        markUp.setId(UUID.randomUUID());
        markUp.setHeight(10);
        markUp.setWidth(10);
        markUp.setXCoordinate(20);
        markUp.setYCoordinate(30);
        return markUp;
    }

    private MarkUpDTO createMarkUpDTO() {
        MarkUpDTO markUpDTO = new MarkUpDTO();
        markUpDTO.setDocumentId(UUID.randomUUID());
        markUpDTO.setId(UUID.randomUUID());
        markUpDTO.setHeight(10);
        markUpDTO.setWidth(10);
        markUpDTO.setXCoordinate(20);
        markUpDTO.setYCoordinate(30);
        return markUpDTO;
    }
}
