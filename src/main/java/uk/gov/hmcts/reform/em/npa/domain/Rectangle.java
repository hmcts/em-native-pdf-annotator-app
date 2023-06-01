package uk.gov.hmcts.reform.em.npa.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

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
    //CHECKSTYLE:OFF
    private Double x;
    //CHECKSTYLE:ON

    @Column(name = "y_coordinate", nullable = false)
    //CHECKSTYLE:OFF
    private Double y;
    //CHECKSTYLE:ON

    @Column(nullable = false)
    private Double width;

    @Column(nullable = false)
    private Double height;

    @ManyToOne
    @JsonIgnoreProperties("rectangles")
    private Redaction redaction;

}
