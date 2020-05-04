package uk.gov.hmcts.reform.em.npa.service.mapper;

import org.mapstruct.Mapper;
import uk.gov.hmcts.reform.em.npa.domain.Redaction;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RedactionDTO;

/**
 * Mapper for the entity Rectangle and its DTO MarkUpDTO.
 */
@Mapper(componentModel = "spring")
public interface MarkUpMapper extends EntityMapper<RedactionDTO, Redaction> {
}
