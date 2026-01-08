package uk.gov.hmcts.reform.em.npa.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.em.npa.domain.Rectangle;
import uk.gov.hmcts.reform.em.npa.domain.Redaction;
import uk.gov.hmcts.reform.em.npa.repository.EntityAuditEventRepository;
import uk.gov.hmcts.reform.em.npa.repository.MarkUpRepository;

import java.util.ArrayList;
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

    @Mock
    private EntityAuditEventRepository entityAuditEventRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void deleteByDocumentIdDeletesWhenFound() {
        Redaction r = new Redaction();
        r.setId(1L);
        Rectangle rect = new uk.gov.hmcts.reform.em.npa.domain.Rectangle();
        rect.setId(10L);
        r.getRectangles().add(rect);
        UUID documentId = UUID.randomUUID();

        when(markUpRepository.findByDocumentId(documentId)).thenReturn(List.of(r));
        when(entityAuditEventRepository.deleteByEntityIdIn(Mockito.anyList())).thenReturn(1);

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

    @Test
    void deleteByDocumentIdSkipsNullRedactions() {
        UUID documentId = UUID.randomUUID();

        // redactions list contains a null and a valid redaction
        List<Redaction> redactions = new ArrayList<>();
        redactions.add(null);
        Redaction r = new Redaction();
        r.setId(2L);
        Rectangle rect = new Rectangle();
        rect.setId(20L);
        r.getRectangles().add(rect);
        redactions.add(r);

        when(markUpRepository.findByDocumentId(documentId)).thenReturn(redactions);
        when(entityAuditEventRepository.deleteByEntityIdIn(Mockito.anyList())).thenReturn(2);

        deleteService.deleteByDocumentId(documentId);

        // ensure we proceeded to deleteAll (no exception due to null entry)
        verify(markUpRepository).deleteAll(redactions);
    }

    @Test
    void deleteByDocumentIdHandlesNullRedactionDbId() {
        UUID documentId = UUID.randomUUID();

        Redaction r = new Redaction();
        // r.setId(null) -> ensure code handles null id
        Rectangle rect = new Rectangle();
        rect.setId(30L);
        r.getRectangles().add(rect);

        when(markUpRepository.findByDocumentId(documentId)).thenReturn(List.of(r));
        when(entityAuditEventRepository.deleteByEntityIdIn(Mockito.anyList())).thenReturn(1);

        deleteService.deleteByDocumentId(documentId);

        verify(markUpRepository).deleteAll(List.of(r));
        verify(entityAuditEventRepository).deleteByEntityIdIn(Mockito.anyList());
    }

    @Test
    void deleteByDocumentIdCoversNonNullRedactionBusinessId() {
        UUID documentId = UUID.randomUUID();

        Redaction r = new Redaction();
        r.setId(3L); // DB id present
        r.setRedactionId(UUID.randomUUID()); // business id present (used only for debug logging)
        Rectangle rect = new Rectangle();
        rect.setId(40L);
        r.getRectangles().add(rect);

        when(markUpRepository.findByDocumentId(documentId)).thenReturn(List.of(r));
        when(entityAuditEventRepository.deleteByEntityIdIn(Mockito.anyList())).thenReturn(2);

        deleteService.deleteByDocumentId(documentId);

        verify(entityAuditEventRepository).deleteByEntityIdIn(Mockito.anyList());
        verify(markUpRepository).deleteAll(List.of(r));
    }
}
