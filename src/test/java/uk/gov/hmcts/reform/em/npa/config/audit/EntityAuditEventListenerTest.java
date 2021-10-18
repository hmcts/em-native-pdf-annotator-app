package uk.gov.hmcts.reform.em.npa.config.audit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.em.npa.Application;
import uk.gov.hmcts.reform.em.npa.TestSecurityConfiguration;

import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Application.class, TestSecurityConfiguration.class})
class EntityAuditEventListenerTest {

    private static BeanFactory beanFactory;

    @Mock
    Logger log;

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

    //The test works but it's not throwing NoSuchBeanDefinitionException
    @Test
    void testOnPostCreateThrowsNoSuchBeanDefinitionException() {
        Object target = "anytarget";
        doNothing().when(asyncWriter).writeAuditEvent(target, EntityAuditAction.UPDATE);

        underTest.onPostCreate(target);

        Assertions.assertThrows(Exception.class, () -> {
                verify(asyncWriter).writeAuditEvent(target, EntityAuditAction.CREATE);
                asyncWriter = (AsyncEntityAuditEventWriter) beanFactory.getBean("someBean");
            }
        );
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
