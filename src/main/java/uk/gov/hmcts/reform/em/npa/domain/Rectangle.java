package uk.gov.hmcts.reform.em.npa.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;

import javax.persistence.*;
import java.io.Serializable;
import java.util.UUID;

/**
 * A Rectangle.
 */
@Entity
@Table(name = "rectangle")
@Getter
@Setter
public class Rectangle implements Serializable {

    @Id
    private UUID id;

    @CreatedBy
    @Column(name = "created_by", nullable = false, length = 50, updatable = false)
    private String createdBy;

    @Column(name = "x_coordinate", nullable = false)
    private Double x;

    @Column(name = "y_coordinate", nullable = false)
    private Double y;

    @Column(nullable = false)
    private Double width;

    @Column(nullable = false)
    private Double height;

    @ManyToOne
    @JsonIgnoreProperties("rectangles")
    private Redaction redaction;

}
