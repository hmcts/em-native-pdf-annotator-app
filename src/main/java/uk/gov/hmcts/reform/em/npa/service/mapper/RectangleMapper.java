package uk.gov.hmcts.reform.em.npa.service.mapper;

import org.mapstruct.Mapper;
import uk.gov.hmcts.reform.em.npa.domain.Rectangle;
import uk.gov.hmcts.reform.em.npa.service.dto.redaction.RectangleDTO;

/**
 * Mapper for the entity Rectangle and its DTO RectangleDTO.
 */
@Mapper(componentModel = "spring", uses = {MarkUpMapper.class})
public interface RectangleMapper extends EntityMapper<RectangleDTO, Rectangle> {

}
