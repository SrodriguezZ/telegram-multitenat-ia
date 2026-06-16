package com.telegram.ia.telegramlink.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "clients", indexes = @Index(name = "idx_clients_company", columnList = "company_id"))
public class ClientJpaEntity {
    @Id private UUID id;
    @Column(name = "company_id", nullable = false) private UUID companyId;
    @Column(name = "external_reference", length = 100) private String externalReference;
    @Column(name = "document_type", length = 30) private String documentType;
    @Column(name = "document_number", length = 50) private String documentNumber;
    @Column(name = "full_name", nullable = false, length = 180) private String fullName;
    @Column(name = "phone_number", length = 40) private String phoneNumber;
    @Column(length = 255) private String email;
    @Column(nullable = false, length = 30) private String status;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected ClientJpaEntity() {}

    public ClientJpaEntity(UUID id, UUID companyId, String externalReference, String documentType, String documentNumber, String fullName, String phoneNumber, String email, String status, Instant createdAt, Instant updatedAt) {
        this.id = id; this.companyId = companyId; this.externalReference = externalReference; this.documentType = documentType; this.documentNumber = documentNumber; this.fullName = fullName; this.phoneNumber = phoneNumber; this.email = email; this.status = status; this.createdAt = createdAt; this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public String getFullName() { return fullName; }
    public String getStatus() { return status; }
}
