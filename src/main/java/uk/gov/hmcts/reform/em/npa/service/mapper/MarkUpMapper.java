package uk.gov.hmcts.reform.em.npa.service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.hmcts.reform.em.npa.domain.Redaction;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RedactionDTO;

/**
 * Mapper for the entity Rectangle and its DTO RedactionDTO.
 */
@Mapper(componentModel = "spring", uses = {RectangleMapper.class})
public interface MarkUpMapper extends EntityMapper<RedactionDTO, Redaction> {

    @Mapping(target = "rectangles")
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "lastModifiedDate", ignore = true)
    Redaction toEntity(RedactionDTO dto);

}
