package uk.gov.hmcts.reform.em.npa.service.dto.external.annotation;

import uk.gov.hmcts.reform.em.npa.service.dto.AbstractAuditingDTO;

import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the Rectangle entity.
 */
public class RectangleDTO extends AbstractAuditingDTO implements Serializable {

    private Long id;

    private Integer x;

    private Integer y;

    private Integer width;

    private Integer height;

    private Long annotationId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getX() {
        return x;
    }

    public void setX(Integer x) {
        this.x = x;
    }

    public Integer getY() {
        return y;
    }

    public void setY(Integer y) {
        this.y = y;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Long getAnnotationId() {
        return annotationId;
    }

    public void setAnnotationId(Long annotationId) {
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
