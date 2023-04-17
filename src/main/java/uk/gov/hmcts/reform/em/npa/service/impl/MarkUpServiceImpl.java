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
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RedactionSetDTO;
import uk.gov.hmcts.reform.em.npa.service.mapper.MarkUpMapper;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
                             SecurityUtils securityUtils){
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

        if(CollectionUtils.isNotEmpty(redaction.getRectangles())) {
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

    @Override
    public RedactionSetDTO saveAll(RedactionSetDTO redactionSetDTO) {
        log.debug("Request to save Redaction Set : {}", redactionSetDTO);

        Set<Redaction> redactionSet = markUpMapper.toEntity(redactionSetDTO.getSearchRedactions());
        redactionSet
                .forEach(this::setCreatedData);

        Set<Redaction> savedRedactions = Set.copyOf(markUpRepository.saveAll(redactionSet));
        Set<RedactionDTO> redactionDTOS = markUpMapper.toDto(savedRedactions);

        return new RedactionSetDTO(redactionDTOS);
    }

    private void setCreatedData(Redaction redaction) {
        String createdBy = securityUtils.getCurrentUserLogin()
                .orElseThrow(() -> new UsernameNotFoundException(USER_NOT_FOUND));
        redaction.setCreatedBy(createdBy);
        setRectangleData(redaction, createdBy);
    }

    private void setRectangleData(Redaction redaction, String createdBy) {
        if(CollectionUtils.isNotEmpty(redaction.getRectangles())) {
            redaction.getRectangles()
                    .forEach(rectangle -> {
                        rectangle.setCreatedBy(createdBy);
                        rectangle.setRedaction(redaction);
                    });
        }
    }
}
