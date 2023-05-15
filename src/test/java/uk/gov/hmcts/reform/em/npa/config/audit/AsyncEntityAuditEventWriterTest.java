package uk.gov.hmcts.reform.em.npa.config.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.em.npa.domain.EntityAuditEvent;
import uk.gov.hmcts.reform.em.npa.domain.Redaction;
import uk.gov.hmcts.reform.em.npa.repository.EntityAuditEventRepository;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class AsyncEntityAuditEventWriterTest {

    @Mock
    private EntityAuditEventRepository repository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AsyncEntityAuditEventWriter asyncEntityAuditEventWriter;

    @Test
    public void testWriteAuditEventWithDebug() {
        Object target = new Redaction();

        asyncEntityAuditEventWriter.writeAuditEvent(target, EntityAuditAction.CREATE);

        assertSame(EntityAuditAction.CREATE.toString(), "CREATE");
        verify(repository).save(new EntityAuditEvent());
    }

    @Test
    public void testWriteAuditEventModifyAuditAction() {
        Object target = new Redaction();

        asyncEntityAuditEventWriter.writeAuditEvent(target, EntityAuditAction.UPDATE);

        verify(repository).save(new EntityAuditEvent());
    }

    @Test
    public void testDeleteAuditEventModifyAuditAction() {
        Object target = new Redaction();

        asyncEntityAuditEventWriter.writeAuditEvent(target, EntityAuditAction.DELETE);

        verify(repository).save(new EntityAuditEvent());
    }

    @Test
    public void testWriteAuditEventException() {
        asyncEntityAuditEventWriter.writeAuditEvent(new Object(), EntityAuditAction.CREATE);
        Assertions.assertThrows(Exception.class, () ->
                verify(asyncEntityAuditEventWriter).writeAuditEvent(new Object(), EntityAuditAction.CREATE));
    }
}
