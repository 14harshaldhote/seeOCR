package com.see.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;
import java.util.UUID;

@Data
@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    @GeneratedValue
    private UUID id;
    @Column(name = "entity_type", nullable = false)
    private String entityType;
    @Column(name = "entity_id", nullable = false)
    private UUID entityId;
    @Column(name = "action", nullable = false)
    private String action;
    @ManyToOne
    @JoinColumn(name = "performed_by")
    private User performedBy;
    @Column(columnDefinition = "TEXT")
    private String details;
    @Column(name = "timestamp")
    private Date timestamp = new Date();

}