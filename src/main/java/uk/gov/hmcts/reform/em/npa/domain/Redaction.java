package uk.gov.hmcts.reform.em.npa.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * A Redaction.
 */
@Entity
@Table(name = "redaction")
@Getter
@Setter
public class Redaction extends AbstractAuditingEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(name = "redaction_id", nullable = false)
    private UUID redactionId;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "page_num", nullable = false)
    private Integer page;

    @OneToMany(mappedBy = "redaction", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Rectangle> rectangles = new HashSet<>();

}
