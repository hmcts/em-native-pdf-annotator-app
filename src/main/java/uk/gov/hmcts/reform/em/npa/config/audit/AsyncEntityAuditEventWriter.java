package uk.gov.hmcts.reform.em.npa.config.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.em.npa.domain.AbstractAuditingEntity;
import uk.gov.hmcts.reform.em.npa.domain.EntityAuditEvent;
import uk.gov.hmcts.reform.em.npa.repository.EntityAuditEventRepository;
import uk.gov.hmcts.reform.em.npa.rest.errors.EntityAuditEventException;

import java.io.IOException;

/**
 * Async Entity Audit Event writer.
 * This is invoked by Hibernate entity listeners to write audit event for entities
 */
@Component
public class AsyncEntityAuditEventWriter {

    private final Logger log = LoggerFactory.getLogger(AsyncEntityAuditEventWriter.class);

    private final EntityAuditEventRepository auditingEntityRepository;

    private final ObjectMapper objectMapper; //Jackson object mapper

    public AsyncEntityAuditEventWriter(EntityAuditEventRepository auditingEntityRepository, ObjectMapper objectMapper) {
        this.auditingEntityRepository = auditingEntityRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Writes audit events to DB asynchronously in a new thread.
     */
    @Async
    public void writeAuditEvent(AbstractAuditingEntity target, EntityAuditAction action) {
        if (log.isDebugEnabled()) {
            log.debug("-------------- Post {} audit  --------------", action.value());
        }
        try {
            EntityAuditEvent auditedEntity = prepareAuditEntity(target, action);
            auditingEntityRepository.save(auditedEntity);
        } catch (Exception e) {
            log.error("Exception while getting entity ID and content {}", e.getMessage(), e);
        }
    }

    /**
     * Method to prepare auditing entity.
     *
     * @param entity to write
     * @param action audit action
     * @return EntityAuditEvent
     */
    private EntityAuditEvent prepareAuditEntity(
            final AbstractAuditingEntity entity,
            EntityAuditAction action
    ) throws EntityAuditEventException {
        EntityAuditEvent auditedEntity = new EntityAuditEvent();
        Class<?> entityClass = entity.getClass(); // Retrieve entity class with reflection
        auditedEntity.setAction(action.value());
        auditedEntity.setEntityType(entityClass.getName());
        String entityData;
        log.trace("Getting Content");
        try {
            entityData = objectMapper.writeValueAsString(entity);
        } catch (IllegalArgumentException
                | SecurityException
                | IOException e) {
            // instead of returning null, a custom exception is thrown, and it is caught in the writeAuditEvent method
            throw new EntityAuditEventException(e.getMessage());
        }
        auditedEntity.setEntityId(entity.getId());
        auditedEntity.setEntityValueV2(entityData);
        if (EntityAuditAction.CREATE.equals(action)) {
            auditedEntity.setModifiedBy(entity.getCreatedBy());
            auditedEntity.setModifiedDate(entity.getCreatedDate());
            auditedEntity.setCommitVersion(1);
        } else {
            auditedEntity.setModifiedBy(entity.getLastModifiedBy());
            auditedEntity.setModifiedDate(entity.getLastModifiedDate());
            calculateVersion(auditedEntity);
        }
        if (log.isTraceEnabled()) {
            log.trace("Audit Entity --> {} ", auditedEntity);
        }
        return auditedEntity;
    }

    private void calculateVersion(EntityAuditEvent auditedEntity) {
        log.trace("Version calculation. for update/remove");
        Integer lastCommitVersion = auditingEntityRepository.findMaxCommitVersion(auditedEntity
            .getEntityType(), auditedEntity.getEntityId());
        log.trace("Last commit version of entity => {}", lastCommitVersion);
        if (lastCommitVersion != null && lastCommitVersion != 0) {
            log.trace("Present. Adding version..");
            auditedEntity.setCommitVersion(lastCommitVersion + 1);
        } else {
            log.trace("No entities.. Adding new version 1");
            auditedEntity.setCommitVersion(1);
        }
    }
}
