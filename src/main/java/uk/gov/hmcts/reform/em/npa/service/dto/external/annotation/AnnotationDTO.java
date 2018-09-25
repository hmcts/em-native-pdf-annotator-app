package uk.gov.hmcts.reform.em.npa.service.dto.external.annotation;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.hmcts.reform.em.npa.service.dto.AbstractAuditingDTO;

import java.io.Serializable;
import java.util.Objects;
import java.util.Set;

/**
 * A DTO for the Annotation entity.
 */
public class AnnotationDTO extends AbstractAuditingDTO implements Serializable {

    private Long id;

    @JsonProperty("type")
    private AnnotationType annotationType;

    private Integer page;

    private Integer x;

    private Integer y;

    private Integer width;

    private Integer height;

    private Long annotationSetId;

    private Set<CommentDTO> comments;

    private Set<RectangleDTO> rectangles;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AnnotationType getAnnotationType() {
        return annotationType;
    }

    public void setAnnotationType(AnnotationType annotationType) {
        this.annotationType = annotationType;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
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

    public Long getAnnotationSetId() {
        return annotationSetId;
    }

    public void setAnnotationSetId(Long annotationSetId) {
        this.annotationSetId = annotationSetId;
    }

    public Set<CommentDTO> getComments() {
        return comments;
    }

    public void setComments(Set<CommentDTO> comments) {
        this.comments = comments;
    }

    public Set<RectangleDTO> getRectangles() {
        return rectangles;
    }

    public void setRectangles(Set<RectangleDTO> rectangles) {
        this.rectangles = rectangles;
    }

    @Override

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AnnotationDTO annotationDTO = (AnnotationDTO) o;
        if (annotationDTO.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), annotationDTO.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "AnnotationDTO{" +
            "id=" + getId() +
            ", annotationType='" + getAnnotationType() + "'" +
            ", page=" + getPage() +
            ", x=" + getX() +
            ", y=" + getY() +
            ", width=" + getWidth() +
            ", height=" + getHeight() +
            ", annotationSet=" + getAnnotationSetId() +
            "}";
    }
}
