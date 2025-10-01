package com.see.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.util.*;

@Data
@Entity
@Table(name = "document_fields")
public class DocumentField {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "document_id")
    private Document document;

    @Column(name = "field_name", nullable = false)
    private String fieldName;

    @Column(name = "field_value")
    private String fieldValue;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "created_at")
    private Date createdAt = new Date();


}
