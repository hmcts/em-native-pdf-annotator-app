package uk.gov.hmcts.reform.em.npa.config.audit;

import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.em.npa.domain.AbstractAuditingEntity;


@Component
public class EntityAuditEventListener extends AuditingEntityListener {

    private static BeanFactory beanFactory;

    @PostPersist
    public void onPostCreate(AbstractAuditingEntity target) {
        AsyncEntityAuditEventWriter asyncEntityAuditEventWriter
                = beanFactory.getBean(AsyncEntityAuditEventWriter.class);
        asyncEntityAuditEventWriter.writeAuditEvent(target, EntityAuditAction.CREATE);
    }

    @PostUpdate
    public void onPostUpdate(AbstractAuditingEntity target) {
        AsyncEntityAuditEventWriter asyncEntityAuditEventWriter
                = beanFactory.getBean(AsyncEntityAuditEventWriter.class);
        asyncEntityAuditEventWriter.writeAuditEvent(target, EntityAuditAction.UPDATE);
    }

    @PostRemove
    public void onPostRemove(AbstractAuditingEntity target) {
        AsyncEntityAuditEventWriter asyncEntityAuditEventWriter
                = beanFactory.getBean(AsyncEntityAuditEventWriter.class);
        asyncEntityAuditEventWriter.writeAuditEvent(target, EntityAuditAction.DELETE);
    }

    static void setBeanFactory(BeanFactory beanFactory) {
        EntityAuditEventListener.beanFactory = beanFactory;
    }

}
