package uk.gov.hmcts.reform.em.npa.service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import uk.gov.hmcts.reform.em.npa.domain.Redaction;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RedactionDTO;

/**
 * Mapper for the entity Rectangle and its DTO MarkUpDTO.
 */
@Mapper(componentModel = "spring")
public interface MarkUpMapper extends EntityMapper<RedactionDTO, Redaction> {


    @Mappings({
        @Mapping(target="id", source="dto.redactionId")
    })
    Redaction toEntity(RedactionDTO dto);

    @Mappings({
        @Mapping(target="redactionId", source="entity.id")
    })
    RedactionDTO toDto(Redaction entity);
}
