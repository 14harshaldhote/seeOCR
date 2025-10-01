package com.see.domain;

import com.see.constants.DocumentType;
import com.see.constants.QCStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "batch_id")
    private Batch batch;

    @Enumerated(EnumType.STRING)
    private DocumentType type;

    @Column(name = "original_path", nullable = false)
    private String originalPath;

    @Column(name = "thumbnail_path")
    private String thumbnailPath;

    @Column(name = "sha256_hash", unique = true, nullable = false)
    private String sha256Hash;

    @ManyToOne
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "qc_status")
    private QCStatus qcStatus = QCStatus.PENDING;

    @Column(name = "created_at")
    private Date createdAt = new Date();

    @Column(name = "updated_at")
    private Date updatedAt = new Date();

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL)
    private Set<DocumentField> documentFields = new HashSet<>();


}
