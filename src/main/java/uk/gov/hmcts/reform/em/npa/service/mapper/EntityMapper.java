package uk.gov.hmcts.reform.em.npa.service.mapper;

import java.util.List;
import java.util.Set;

/**
 * Contract for a generic dto to entity mapper.
 *
 * @param <D> - DTO type parameter.
 * @param <E> - Entity type parameter.
 */

public interface EntityMapper<D, E> {

    E toEntity(D dto);

    Set<E> toEntity(Set<D> dtoList);

    List<E> toEntity(List<D> dtoList);

    D toDto(E entity);

    Set<D> toDto(Set<E> entityList);

    List<D> toDto(List<E> entityList);
}
