package uk.gov.hmcts.reform.em.npa.service.dto.external.annotation;

import uk.gov.hmcts.reform.em.npa.service.dto.AbstractAuditingDTO;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the Rectangle entity.
 */
public class RectangleDTO extends AbstractAuditingDTO implements Serializable {

    private UUID id;

    private Float x;

    private Float y;

    private Float width;

    private Float height;

    private UUID annotationId;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Float getX() {
        return x;
    }

    public void setX(Float x) {
        this.x = x;
    }

    public Float getY() {
        return y;
    }

    public void setY(Float y) {
        this.y = y;
    }

    public Float getWidth() {
        return width;
    }

    public void setWidth(Float width) {
        this.width = width;
    }

    public Float getHeight() {
        return height;
    }

    public void setHeight(Float height) {
        this.height = height;
    }

    public UUID getAnnotationId() {
        return annotationId;
    }

    public void setAnnotationId(UUID annotationId) {
        this.annotationId = annotationId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RectangleDTO rectangleDTO = (RectangleDTO) o;
        if (rectangleDTO.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), rectangleDTO.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "RectangleDTO{" +
            "id=" + getId() +
            ", x=" + getX() +
            ", y=" + getY() +
            ", width=" + getWidth() +
            ", height=" + getHeight() +
            ", annotation=" + getAnnotationId() +
            "}";
    }
}
