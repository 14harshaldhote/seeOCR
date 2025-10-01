package com.see.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@Entity
@Table(name = "batch")
public class Batch {

    @Id
    @GeneratedValue
    private UUID id;

    private String name;

    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    @Column(name = "created_at")
    private Date createdAt = new Date();

    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL)
    private Set<Document> documents = new HashSet<>();
}
