package com.survisha.meghaconnect.entity;

import javax.persistence.*;
import lombok.*;

@Entity
@Table(name = "scheme_required_documents", 
    indexes = { @Index(name = "idx_scheme_doc_code", columnList = "scheme_code") })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SchemeDocument extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scheme_code", nullable = false, length = 50)
    private String schemeCode; // References reference_data.code where type='CM_SCHEME'

    @Column(name = "document_type", nullable = false, length = 100)
    private String documentType;

    @Column(name = "document_label", nullable = false, length = 200)
    private String documentLabel;

    @Column(name = "is_required", nullable = false)
    private Boolean isRequired = true;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "file_format_allowed", length = 100)
    private String fileFormatAllowed; // e.g., "pdf,jpg,png"

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}
