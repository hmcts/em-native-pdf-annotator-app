package uk.gov.hmcts.reform.em.npa.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.UUID;

/**
 * A MarkUp.
 */
@Entity
@Table(name = "markup")
@Getter
@Setter
public class MarkUp implements Serializable {

    @Id
    private UUID id;

    @Size(min = 1, max = 30)
    @Column(name = "name", length = 30, nullable = false)
    private String name;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @CreatedBy
    @Column(name = "created_by", nullable = false, length = 50, updatable = false)
    private String createdBy;

    @Column(name = "num", nullable = false)
    private Integer pageNumber;

    @Column(name = "x_coordinate")
    private Double xCoordinate;

    @Column(name = "y_coordinate")
    private Double yCoordinate;

}
