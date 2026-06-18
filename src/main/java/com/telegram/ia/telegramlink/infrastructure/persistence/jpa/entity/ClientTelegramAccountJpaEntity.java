package com.telegram.ia.telegramlink.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "client_telegram_accounts", indexes = {
        @Index(name = "idx_account_company_telegram", columnList = "company_id,telegram_user_id"),
        @Index(name = "idx_account_company_client", columnList = "company_id,client_id")},
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_account_company_telegram_status", columnNames = {"company_id", "telegram_user_id", "status"}),
                @UniqueConstraint(name = "uk_account_company_client_status", columnNames = {"company_id", "client_id", "status"})})
public class ClientTelegramAccountJpaEntity {
    @Id private UUID id;
    @Column(name = "company_id", nullable = false) private UUID companyId;
    @Column(name = "client_id", nullable = false) private UUID clientId;
    @Column(name = "telegram_user_id", nullable = false) private Long telegramUserId;
    @Column(name = "telegram_chat_id") private Long telegramChatId;
    @Column(name = "telegram_username", length = 100) private String telegramUsername;
    @Column(name = "telegram_first_name", length = 100) private String telegramFirstName;
    @Column(name = "telegram_last_name", length = 100) private String telegramLastName;
    @Column(name = "linked_by_invitation_token_id") private UUID linkedByInvitationTokenId;
    @Column(nullable = false, length = 30) private String status;
    @Column(name = "linked_at", nullable = false) private Instant linkedAt;
    @Column(name = "unlinked_at") private Instant unlinkedAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected ClientTelegramAccountJpaEntity() {}

    public ClientTelegramAccountJpaEntity(UUID id, UUID companyId, UUID clientId, Long telegramUserId, Long telegramChatId, String telegramUsername, String telegramFirstName, String telegramLastName, UUID linkedByInvitationTokenId, String status, Instant linkedAt, Instant unlinkedAt, Instant createdAt, Instant updatedAt) {
        this.id = id; this.companyId = companyId; this.clientId = clientId; this.telegramUserId = telegramUserId; this.telegramChatId = telegramChatId; this.telegramUsername = telegramUsername; this.telegramFirstName = telegramFirstName; this.telegramLastName = telegramLastName; this.linkedByInvitationTokenId = linkedByInvitationTokenId; this.status = status; this.linkedAt = linkedAt; this.unlinkedAt = unlinkedAt; this.createdAt = createdAt; this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public UUID getClientId() { return clientId; }
    public Long getTelegramUserId() { return telegramUserId; }
    public Long getTelegramChatId() { return telegramChatId; }
    public String getTelegramUsername() { return telegramUsername; }
    public String getTelegramFirstName() { return telegramFirstName; }
    public String getTelegramLastName() { return telegramLastName; }
    public UUID getLinkedByInvitationTokenId() { return linkedByInvitationTokenId; }
    public String getStatus() { return status; }
    public Instant getLinkedAt() { return linkedAt; }
    public Instant getUnlinkedAt() { return unlinkedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
