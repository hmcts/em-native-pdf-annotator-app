package uk.gov.hmcts.reform.em.npa.service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import uk.gov.hmcts.reform.em.npa.domain.Redaction;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RedactionDTO;

/**
 * Mapper for the entity Rectangle and its DTO RedactionDTO.
 */
@Mapper(componentModel = "spring", uses = {RectangleMapper.class})
public interface MarkUpMapper extends EntityMapper<RedactionDTO, Redaction> {

    @Mapping(target="id", source="dto.redactionId")
    @Mapping(target = "rectangles")
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "lastModifiedDate", ignore = true)
    Redaction toEntity(RedactionDTO dto);

    @Mappings({
        @Mapping(target="redactionId", source="entity.id")
    })
    RedactionDTO toDto(Redaction entity);
}
