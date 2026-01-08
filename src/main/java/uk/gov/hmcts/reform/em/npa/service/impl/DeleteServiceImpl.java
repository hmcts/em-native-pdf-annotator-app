package uk.gov.hmcts.reform.em.npa.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;
import uk.gov.hmcts.reform.em.npa.domain.Rectangle;
import uk.gov.hmcts.reform.em.npa.domain.Redaction;
import uk.gov.hmcts.reform.em.npa.repository.EntityAuditEventRepository;
import uk.gov.hmcts.reform.em.npa.repository.MarkUpRepository;
import uk.gov.hmcts.reform.em.npa.service.DeleteService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class DeleteServiceImpl implements DeleteService {

    private static final Logger log = LoggerFactory.getLogger(DeleteServiceImpl.class);

    private final MarkUpRepository markUpRepository;
    private final EntityAuditEventRepository entityAuditEventRepository;

    public DeleteServiceImpl(MarkUpRepository markUpRepository,
                             EntityAuditEventRepository entityAuditEventRepository) {
        this.markUpRepository = markUpRepository;
        this.entityAuditEventRepository = entityAuditEventRepository;
    }

    @Override
    @Transactional
    public void deleteByDocumentId(UUID documentId) {
        log.debug("Deleting all redactions for documentId: {}", documentId);
        var redactions = markUpRepository.findByDocumentId(documentId);
        if (Objects.isNull(redactions) || redactions.isEmpty()) {
            log.debug("No redactions found for document {}", documentId);
            return;
        }

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        List<Long> allAuditIds = extractAllAuditIds(redactions);

        if (!allAuditIds.isEmpty()) {
            int totalAudits = entityAuditEventRepository.deleteByEntityIdIn(allAuditIds);
            log.debug("Deleted {} audit events (rectangles + redactions) for document {}",
                    totalAudits, documentId);
        } else {
            log.info("No rectangle or redaction audit events found for document {}", documentId);
        }

        log.debug("Found {} redactions for document {}",
                redactions.size(), documentId);
        markUpRepository.deleteAll(redactions);
        stopWatch.stop();
        log.info("Deletion completed for document {} in {} ms", documentId, stopWatch.getTotalTimeMillis());
    }

    private List<Long> extractAllAuditIds(List<Redaction> redactions) {
        List<Long> allAuditIds = new ArrayList<>();
        for (Redaction redaction : redactions) {
            if (Objects.isNull(redaction)) {
                continue;
            }
            if (Objects.nonNull(redaction.getId())) {
                allAuditIds.add(redaction.getId());
            }
            for (Rectangle rectangle : redaction.getRectangles()) {
                if (Objects.nonNull(rectangle.getId())) {
                    allAuditIds.add(rectangle.getId());
                }
            }
        }
        return allAuditIds;
    }
}
