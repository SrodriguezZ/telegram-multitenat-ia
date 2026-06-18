package com.telegram.ia.telegramlink.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "companies")
public class CompanyJpaEntity {
    @Id private UUID id;
    @Column(nullable = false, length = 150) private String name;
    @Column(name = "legal_name", length = 200) private String legalName;
    @Column(nullable = false, length = 30) private String status;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected CompanyJpaEntity() {}

    public CompanyJpaEntity(UUID id, String name, String legalName, String status, Instant createdAt, Instant updatedAt) {
        this.id = id; this.name = name; this.legalName = legalName; this.status = status; this.createdAt = createdAt; this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getLegalName() { return legalName; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
