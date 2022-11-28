package uk.gov.hmcts.reform.em.npa.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.UUID;

/**
 * A Rectangle.
 */
@Entity
@Table(name = "rectangle")
@Getter
@Setter
public class Rectangle extends AbstractAuditingEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(name = "rectangle_id", nullable = false)
    private UUID rectangleId;

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
