package uk.gov.hmcts.reform.em.npa.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.em.npa.domain.Rectangle;
import uk.gov.hmcts.reform.em.npa.domain.Redaction;
import uk.gov.hmcts.reform.em.npa.repository.EntityAuditEventRepository;
import uk.gov.hmcts.reform.em.npa.repository.MarkUpRepository;
import uk.gov.hmcts.reform.em.npa.service.DeleteService;

import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

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
        if (Objects.nonNull(redactions) && !redactions.isEmpty()) {
            final long startMs = System.currentTimeMillis();
            log.info("Deletion timing - start for document {}", documentId);
            var rectangleIds = redactions.stream()
                .filter(Objects::nonNull)
                .flatMap(r -> r.getRectangles().stream())
                .map(Rectangle::getId)
                .filter(Objects::nonNull);

            var redactionDbIds = redactions.stream()
                .map(Redaction::getId)
                .filter(Objects::nonNull);

            var allAuditIds = java.util.stream.Stream.concat(rectangleIds, redactionDbIds)
                .toList();

            if (!allAuditIds.isEmpty()) {
                int totalAudits = entityAuditEventRepository.deleteByEntityIdIn(allAuditIds);
                log.debug("Deleted {} audit events (rectangles + redactions) for document {}",
                    totalAudits, documentId);
            } else {
                log.info("No rectangle or redaction audit events found for document {}", documentId);
            }

            log.debug("Found {} redactions for document {}: {}",
                redactions.size(),
                documentId,
                redactions.stream()
                        .filter(r -> r.getRedactionId() != null)
                        .map(r -> r.getRedactionId().toString()).collect(Collectors.joining(", ")));
            markUpRepository.deleteAll(redactions);
            long elapsedMs = System.currentTimeMillis() - startMs;
            log.info("Deletion timing - completed for document {} in {} ms", documentId, elapsedMs);
        } else {
            log.debug("No redactions found for document {}", documentId);
        }

    }
}
