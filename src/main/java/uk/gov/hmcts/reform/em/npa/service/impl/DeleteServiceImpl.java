package uk.gov.hmcts.reform.em.npa.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.em.npa.repository.RedactionRepository;
import uk.gov.hmcts.reform.em.npa.service.DeleteService;

import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DeleteServiceImpl implements DeleteService {

    private static final Logger log = LoggerFactory.getLogger(DeleteServiceImpl.class);

    private final RedactionRepository redactionRepository;

    public DeleteServiceImpl(RedactionRepository redactionRepository) {
        this.redactionRepository = redactionRepository;
    }

    @Override
    @Transactional
    public void deleteByDocumentId(UUID documentId) {
        log.debug("Deleting all redactions for documentId: {}", documentId);
        var redactions = redactionRepository.findByDocumentId(documentId);
        if (Objects.nonNull(redactions) && !redactions.isEmpty()) {
            log.debug("Found {} redactions for document {}: {}",
                redactions.size(),
                documentId,
                redactions.stream()
                        .filter(r -> r.getRedactionId() != null)
                        .map(r -> r.getRedactionId().toString()).collect(Collectors.joining(", ")));
            redactionRepository.deleteAll(redactions);
        } else {
            log.debug("No redactions found for document {}", documentId);
        }

    }
}
