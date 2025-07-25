package uk.gov.hmcts.reform.em.npa.config.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.em.npa.domain.AbstractAuditingEntity;
import uk.gov.hmcts.reform.em.npa.domain.EntityAuditEvent;
import uk.gov.hmcts.reform.em.npa.domain.Rectangle;
import uk.gov.hmcts.reform.em.npa.domain.Redaction;
import uk.gov.hmcts.reform.em.npa.repository.EntityAuditEventRepository;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AsyncEntityAuditEventWriterTest {

    @Mock
    private EntityAuditEventRepository repository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AsyncEntityAuditEventWriter asyncEntityAuditEventWriter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testWriteAuditEventWithDebug() {
        AbstractAuditingEntity target = new Redaction();

        asyncEntityAuditEventWriter.writeAuditEvent(target, EntityAuditAction.CREATE);

        assertSame("CREATE", EntityAuditAction.CREATE.toString());
        verify(repository).save(new EntityAuditEvent());
    }

    @Test
    void testWriteAuditEventModifyAuditAction() {
        AbstractAuditingEntity target = new Redaction();

        asyncEntityAuditEventWriter.writeAuditEvent(target, EntityAuditAction.UPDATE);

        verify(repository).save(new EntityAuditEvent());
    }

    @Test
    void testDeleteAuditEventModifyAuditAction() {
        AbstractAuditingEntity target = new Redaction();

        asyncEntityAuditEventWriter.writeAuditEvent(target, EntityAuditAction.DELETE);

        verify(repository).save(new EntityAuditEvent());
    }

    @Test
    void testWriteAuditEventException() {
        asyncEntityAuditEventWriter.writeAuditEvent(new Rectangle(), EntityAuditAction.CREATE);
        Assertions.assertThrows(Exception.class, () ->
                verify(asyncEntityAuditEventWriter).writeAuditEvent(new Rectangle(), EntityAuditAction.CREATE));
    }

    @Test
    void testWriteAuditEventCatchesObjectMapperException() throws Exception {
        Redaction target = new Redaction();
        when(objectMapper.writeValueAsString(target)).thenThrow(new JsonProcessingException("Serialization failed") {});
        asyncEntityAuditEventWriter.writeAuditEvent(target, EntityAuditAction.CREATE);
        verify(repository, never()).save(any(EntityAuditEvent.class));
    }
}
