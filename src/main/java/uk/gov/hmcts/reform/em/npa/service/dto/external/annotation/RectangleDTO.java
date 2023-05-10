package uk.gov.hmcts.reform.em.npa.service.dto.external.annotation;

import lombok.ToString;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * A DTO for the Rectangle entity.
 */
@ToString
public class RectangleDTO extends AbstractAuditingDTO implements Serializable {

    private UUID id;

    //CHECKSTYLE:OFF
    private Double x;
    //CHECKSTYLE:ON


    //CHECKSTYLE:OFF
    private Double y;
    //CHECKSTYLE:ON


    private Double width;

    private Double height;

    private UUID annotationId;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Double getX() {
        return x;
    }

    public void setX(Double x) {
        this.x = x;
    }

    public Double getY() {
        return y;
    }

    public void setY(Double y) {
        this.y = y;
    }

    public Double getWidth() {
        return width;
    }

    public void setWidth(Double width) {
        this.width = width;
    }

    public Double getHeight() {
        return height;
    }

    public void setHeight(Double height) {
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

}
