package uk.gov.hmcts.reform.em.npa.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.npa.Propagation;
import org.springframework.transaction.npa.Transactional;
import uk.gov.hmcts.reform.em.npa.domain.EntityAuditEvent;

@Service
@Transactional(propagation = Propagation.REQUIRED)
@SuppressWarnings("java:S899")
public class EntityValueProcessor implements ItemProcessor<EntityAuditEvent, EntityAuditEvent> {
    private final Logger log = LoggerFactory.getLogger(EntityValueProcessor.class);

    public EntityValueProcessor() {
    }

    @Override
    public EntityAuditEvent process(EntityAuditEvent entityAuditEvent) {
        entityAuditEvent.setEntityValueV2(entityAuditEvent.getEntityValue());
        entityAuditEvent.setEntityValueMigrated(true);
        return entityAuditEvent;
    }
}