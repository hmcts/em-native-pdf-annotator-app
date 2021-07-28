package uk.gov.hmcts.reform.em.npa.config.audit;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.em.npa.Application;
import uk.gov.hmcts.reform.em.npa.TestSecurityConfiguration;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Application.class, TestSecurityConfiguration.class})
class EntityAuditEventListenerTest {

    @MockBean
    private AsyncEntityAuditEventWriter asyncWriter;

    @InjectMocks
    private EntityAuditEventListener underTest;

    @Test
    void testOnPostCreate() {
        Object target = "anytarget";
        doNothing().when(asyncWriter).writeAuditEvent(target, EntityAuditAction.CREATE);

        underTest.onPostCreate(target);

        verify(asyncWriter).writeAuditEvent(target, EntityAuditAction.CREATE);
    }

    @Test
    void testOnPostUpdate() {
        Object target = "anytarget";
        doNothing().when(asyncWriter).writeAuditEvent(target, EntityAuditAction.UPDATE);

        underTest.onPostUpdate(target);

        verify(asyncWriter).writeAuditEvent(target, EntityAuditAction.UPDATE);
    }

    @Test
    void testOnPostRemove() {
        Object target = "anytarget";
        doNothing().when(asyncWriter).writeAuditEvent(target, EntityAuditAction.DELETE);

        underTest.onPostRemove(target);

        verify(asyncWriter).writeAuditEvent(target, EntityAuditAction.DELETE);
    }
}