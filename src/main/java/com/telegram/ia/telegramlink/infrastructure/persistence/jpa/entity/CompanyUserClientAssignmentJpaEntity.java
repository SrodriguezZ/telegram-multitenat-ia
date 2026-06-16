package com.telegram.ia.telegramlink.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "company_user_client_assignments", indexes = @Index(name = "idx_assignments_user_client", columnList = "company_user_id,client_id"))
public class CompanyUserClientAssignmentJpaEntity {
    @Id private UUID id;
    @Column(name = "company_id", nullable = false) private UUID companyId;
    @Column(name = "company_user_id", nullable = false) private UUID companyUserId;
    @Column(name = "client_id", nullable = false) private UUID clientId;
    @Column(nullable = false, length = 30) private String status;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected CompanyUserClientAssignmentJpaEntity() {}

    public CompanyUserClientAssignmentJpaEntity(UUID id, UUID companyId, UUID companyUserId, UUID clientId, String status, Instant createdAt, Instant updatedAt) {
        this.id = id; this.companyId = companyId; this.companyUserId = companyUserId; this.clientId = clientId; this.status = status; this.createdAt = createdAt; this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public UUID getCompanyUserId() { return companyUserId; }
    public UUID getClientId() { return clientId; }
    public String getStatus() { return status; }
}
