package uk.gov.hmcts.reform.em.npa.service.impl;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.em.npa.config.security.SecurityUtils;
import uk.gov.hmcts.reform.em.npa.domain.Redaction;
import uk.gov.hmcts.reform.em.npa.repository.MarkUpRepository;
import uk.gov.hmcts.reform.em.npa.service.MarkUpService;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RedactionDTO;
import uk.gov.hmcts.reform.em.npa.service.mapper.MarkUpMapper;

import java.util.UUID;

/**
 * Service Implementation for managing MarkUps.
 */
@Service
@Transactional
public class MarkUpServiceImpl implements MarkUpService {

    public static final String USER_NOT_FOUND = "User not found.";

    private final Logger log = LoggerFactory.getLogger(MarkUpServiceImpl.class);

    private MarkUpRepository markUpRepository;

    private MarkUpMapper markUpMapper;

    private SecurityUtils securityUtils;

    public MarkUpServiceImpl(MarkUpRepository markUpRepository,
                             MarkUpMapper markUpMapper,
                             SecurityUtils securityUtils) {
        this.markUpRepository = markUpRepository;
        this.markUpMapper = markUpMapper;
        this.securityUtils = securityUtils;
    }

    @Override
    public RedactionDTO save(RedactionDTO redactionDTO) {
        log.debug("Request to save Rectangle : {}", redactionDTO);

        final Redaction redaction = markUpMapper.toEntity(redactionDTO);
        String createdBy = securityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new UsernameNotFoundException(USER_NOT_FOUND));
        redaction.setCreatedBy(createdBy);
        redaction.getRectangles().stream().forEach(rectangle -> rectangle.setCreatedBy(createdBy));

        if (CollectionUtils.isNotEmpty(redaction.getRectangles())) {
            redaction.getRectangles()
                .stream()
                .forEach(rectangle -> rectangle.setRedaction(redaction));
        }

        Redaction response = markUpRepository.save(redaction);

        return markUpMapper.toDto(response);
    }

    @Override
    public Page<RedactionDTO> findAllByDocumentId(UUID documentId, Pageable pageable) {

        String user = securityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new UsernameNotFoundException(USER_NOT_FOUND));

        return markUpRepository.findByDocumentIdAndCreatedBy(documentId, user, pageable)
                    .map(markUpMapper::toDto);
    }

    @Override
    public void deleteAll(UUID documentId) {

        log.debug("Request to delete all Redactions : {}", documentId);
        String createdBy = securityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new UsernameNotFoundException(USER_NOT_FOUND));
        markUpRepository.deleteAllByDocumentIdAndCreatedBy(documentId, createdBy);

    }

    @Override
    public void delete(UUID redactionId) {

        log.debug("Request to delete Redaction : {}", redactionId);
        markUpRepository.deleteByRedactionId(redactionId);
    }
}
