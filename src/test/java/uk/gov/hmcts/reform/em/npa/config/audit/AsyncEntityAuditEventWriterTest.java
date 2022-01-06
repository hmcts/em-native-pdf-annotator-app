package uk.gov.hmcts.reform.em.npa.config.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.em.npa.Application;
import uk.gov.hmcts.reform.em.npa.TestSecurityConfiguration;
import uk.gov.hmcts.reform.em.npa.domain.EntityAuditEvent;
import uk.gov.hmcts.reform.em.npa.domain.Redaction;
import uk.gov.hmcts.reform.em.npa.repository.EntityAuditEventRepository;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertSame;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Application.class, TestSecurityConfiguration.class})
public class AsyncEntityAuditEventWriterTest {

    @Mock
    Logger log;

    @Mock
    EntityAuditEventRepository repository;

    @Mock
    ObjectMapper objectMapper;

    @InjectMocks
    AsyncEntityAuditEventWriter asyncEntityAuditEventWriter;

    @Test
    public void testWriteAuditEventWithDebug() {
        when(log.isDebugEnabled()).thenReturn(true);
        when(log.isTraceEnabled()).thenReturn(true);
        Object target = new Redaction();

        asyncEntityAuditEventWriter.writeAuditEvent(target, EntityAuditAction.CREATE);

        assertSame(EntityAuditAction.CREATE.toString(), "CREATE");
        verify(repository).save(new EntityAuditEvent());
    }

    @Test
    public void testWriteAuditEventModifyAuditAction() {
        when(log.isDebugEnabled()).thenReturn(false);
        when(log.isTraceEnabled()).thenReturn(false);
        Object target = new Redaction();

        asyncEntityAuditEventWriter.writeAuditEvent(target, EntityAuditAction.UPDATE);

        verify(repository).save(new EntityAuditEvent());
    }

    @Test
    public void testDeleteAuditEventModifyAuditAction() {
        when(log.isDebugEnabled()).thenReturn(true);
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
