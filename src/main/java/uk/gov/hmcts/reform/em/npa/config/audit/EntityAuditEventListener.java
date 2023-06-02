package uk.gov.hmcts.reform.em.npa.config.audit;

import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.stereotype.Component;


@Component
public class EntityAuditEventListener extends AuditingEntityListener {

    //Not used within the listener, but instead, it is used to validate that the bean exists
    @Autowired
    private AsyncEntityAuditEventWriter dummyAsyncEntityAuditEventWriter;

    private static BeanFactory beanFactory;

    @PostPersist
    public void onPostCreate(Object target) {
        AsyncEntityAuditEventWriter asyncEntityAuditEventWriter
                = beanFactory.getBean(AsyncEntityAuditEventWriter.class);
        asyncEntityAuditEventWriter.writeAuditEvent(target, EntityAuditAction.CREATE);
    }

    @PostUpdate
    public void onPostUpdate(Object target) {
        AsyncEntityAuditEventWriter asyncEntityAuditEventWriter
                = beanFactory.getBean(AsyncEntityAuditEventWriter.class);
        asyncEntityAuditEventWriter.writeAuditEvent(target, EntityAuditAction.UPDATE);
    }

    @PostRemove
    public void onPostRemove(Object target) {
        AsyncEntityAuditEventWriter asyncEntityAuditEventWriter
                = beanFactory.getBean(AsyncEntityAuditEventWriter.class);
        asyncEntityAuditEventWriter.writeAuditEvent(target, EntityAuditAction.DELETE);
    }

    static void setBeanFactory(BeanFactory beanFactory) {
        EntityAuditEventListener.beanFactory = beanFactory;
    }

}
