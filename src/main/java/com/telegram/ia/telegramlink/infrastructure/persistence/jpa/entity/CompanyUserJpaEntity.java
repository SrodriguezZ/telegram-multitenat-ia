package com.telegram.ia.telegramlink.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "company_users", indexes = @Index(name = "idx_company_users_company", columnList = "company_id"))
public class CompanyUserJpaEntity {
    @Id private UUID id;
    @Column(name = "company_id", nullable = false) private UUID companyId;
    @Column(nullable = false, length = 255) private String email;
    @Column(name = "full_name", nullable = false, length = 150) private String fullName;
    @Column(nullable = false, length = 40) private String role;
    @Column(nullable = false, length = 30) private String status;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected CompanyUserJpaEntity() {}

    public CompanyUserJpaEntity(UUID id, UUID companyId, String email, String fullName, String role, String status, Instant createdAt, Instant updatedAt) {
        this.id = id; this.companyId = companyId; this.email = email; this.fullName = fullName; this.role = role; this.status = status; this.createdAt = createdAt; this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getRole() { return role; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
