package com.telegram.ia.telegramlink.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "telegram_invitation_tokens", indexes = {
        @Index(name = "idx_invitation_company_client", columnList = "company_id,client_id"),
        @Index(name = "idx_invitation_company_status", columnList = "company_id,status"),
        @Index(name = "idx_invitation_expires_at", columnList = "expires_at")})
public class TelegramInvitationTokenJpaEntity {
    @Id private UUID id;
    @Column(name = "company_id", nullable = false) private UUID companyId;
    @Column(name = "client_id", nullable = false) private UUID clientId;
    @Column(name = "created_by_user_id", nullable = false) private UUID createdByUserId;
    @Column(name = "token_hash", nullable = false, unique = true, length = 255) private String tokenHash;
    @Column(name = "token_prefix", length = 12) private String tokenPrefix;
    @Column(nullable = false, length = 30) private String status;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "used_at") private Instant usedAt;
    @Column(name = "revoked_at") private Instant revokedAt;
    @Column(name = "revoked_by_user_id") private UUID revokedByUserId;
    @Column(name = "used_by_telegram_user_id") private Long usedByTelegramUserId;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected TelegramInvitationTokenJpaEntity() {}

    public TelegramInvitationTokenJpaEntity(UUID id, UUID companyId, UUID clientId, UUID createdByUserId, String tokenHash, String tokenPrefix, String status, Instant expiresAt, Instant usedAt, Instant revokedAt, UUID revokedByUserId, Long usedByTelegramUserId, Instant createdAt, Instant updatedAt) {
        this.id = id; this.companyId = companyId; this.clientId = clientId; this.createdByUserId = createdByUserId; this.tokenHash = tokenHash; this.tokenPrefix = tokenPrefix; this.status = status; this.expiresAt = expiresAt; this.usedAt = usedAt; this.revokedAt = revokedAt; this.revokedByUserId = revokedByUserId; this.usedByTelegramUserId = usedByTelegramUserId; this.createdAt = createdAt; this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public UUID getClientId() { return clientId; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public String getTokenHash() { return tokenHash; }
    public String getTokenPrefix() { return tokenPrefix; }
    public String getStatus() { return status; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getUsedAt() { return usedAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public UUID getRevokedByUserId() { return revokedByUserId; }
    public Long getUsedByTelegramUserId() { return usedByTelegramUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
