package uk.gov.hmcts.reform.em.npa.service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.hmcts.reform.em.npa.domain.Rectangle;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RectangleDTO;

/**
 * Mapper for the entity Rectangle and its DTO RectangleDTO.
 */
@Mapper(componentModel = "spring", uses = {MarkUpMapper.class})
public interface RectangleMapper extends EntityMapper<RectangleDTO, Rectangle> {

    @Mapping(target = "id", source = "entity.rectangleId")
    RectangleDTO toDto(Rectangle entity);

    @Mapping(target = "rectangleId", source = "dto.id")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "redaction", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "lastModifiedDate", ignore = true)
    Rectangle toEntity(RectangleDTO dto);

}
