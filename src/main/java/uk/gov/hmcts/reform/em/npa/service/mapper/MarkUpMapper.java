package uk.gov.hmcts.reform.em.npa.service.mapper;

import org.mapstruct.Mapper;
import uk.gov.hmcts.reform.em.npa.domain.MarkUp;
import uk.gov.hmcts.reform.em.npa.domain.MarkUpDTO;

/**
 * Mapper for the entity MarkUp and its DTO MarkUpDTO.
 */
@Mapper(componentModel = "spring")
public interface MarkUpMapper extends EntityMapper<MarkUpDTO, MarkUp> {
}
