package uk.gov.hmcts.reform.em.npa.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.em.npa.config.security.SecurityUtils;
import uk.gov.hmcts.reform.em.npa.domain.MarkUp;
import uk.gov.hmcts.reform.em.npa.domain.MarkUpDTO;
import uk.gov.hmcts.reform.em.npa.repository.MarkUpRepository;
import uk.gov.hmcts.reform.em.npa.service.MarkUpService;
import uk.gov.hmcts.reform.em.npa.service.mapper.MarkUpMapper;

import java.util.Optional;
import java.util.UUID;

/**
 * Service Implementation for managing MarkUpd.
 */
@Service
@Transactional
public class MarkUpServiceImpl implements MarkUpService {

    private final Logger log = LoggerFactory.getLogger(MarkUpServiceImpl.class);

    private MarkUpRepository markUpRepository;

    private MarkUpMapper markUpMapper;

    private final SecurityUtils securityUtils;

    public MarkUpServiceImpl(MarkUpRepository markUpRepository,
                             MarkUpMapper markUpMapper,
                             SecurityUtils securityUtils){
        this.markUpRepository = markUpRepository;
        this.markUpMapper = markUpMapper;
        this.securityUtils = securityUtils;
    }

    @Override
    public MarkUpDTO save(MarkUpDTO MarkUpDTO) {
        log.debug("Request to save MArkUp : {}", MarkUpDTO);

        MarkUp markUp = markUpMapper.toEntity(MarkUpDTO);
        markUp.setCreatedBy(
                    securityUtils.getCurrentUserLogin()
                            .orElseThrow(() -> new UsernameNotFoundException("User not found."))
            );
        markUp = markUpRepository.save(markUp);

        return markUpMapper.toDto(markUp);
    }

    @Override
    public Page<MarkUpDTO> findAllByDocumentId(UUID documentId, Pageable pageable) {
        Optional<String> user = securityUtils.getCurrentUserLogin();
        if (user.isPresent()) {
            return markUpRepository.findByDocumentIdAndCreatedBy(documentId, user.get(), pageable)
                    .map(markUpMapper::toDto);
        } else {
            throw new UsernameNotFoundException("User not found");
        }
    }

    @Override
    public void delete(UUID id) {
        log.debug("Request to delete MarkUp : {}", id);
        markUpRepository.deleteById(id);
    }
}
