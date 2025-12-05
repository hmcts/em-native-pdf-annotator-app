package uk.gov.hmcts.reform.em.npa.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.em.npa.domain.Redaction;
import uk.gov.hmcts.reform.em.npa.repository.MarkUpRepository;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeleteServiceImplTest {

    @InjectMocks
    private DeleteServiceImpl deleteService;

    @Mock
    private MarkUpRepository markUpRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void deleteByDocumentIdDeletesWhenFound() {
        UUID documentId = UUID.randomUUID();
        Redaction r = new Redaction();
        when(markUpRepository.findByDocumentId(documentId)).thenReturn(List.of(r));

        deleteService.deleteByDocumentId(documentId);

        verify(markUpRepository, Mockito.atMost(1)).deleteAll(List.of(r));
    }

    @Test
    void deleteByDocumentIdNoopWhenEmpty() {
        UUID documentId = UUID.randomUUID();
        when(markUpRepository.findByDocumentId(documentId)).thenReturn(Collections.emptyList());

        deleteService.deleteByDocumentId(documentId);

        verify(markUpRepository, never()).deleteAll(Mockito.anyList());
    }
}
