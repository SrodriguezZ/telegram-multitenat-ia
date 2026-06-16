package com.telegram.ia.telegramlink.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "telegram_link_events", indexes = @Index(name = "idx_link_events_company_client", columnList = "company_id,client_id"))
public class TelegramLinkEventJpaEntity {
    @Id private UUID id;
    @Column(name = "company_id") private UUID companyId;
    @Column(name = "client_id") private UUID clientId;
    @Column(name = "invitation_token_id") private UUID invitationTokenId;
    @Column(name = "company_user_id") private UUID companyUserId;
    @Column(name = "telegram_user_id") private Long telegramUserId;
    @Column(name = "telegram_chat_id") private Long telegramChatId;
    @Column(name = "event_type", nullable = false, length = 50) private String eventType;
    @Column(nullable = false, length = 30) private String result;
    @Column(name = "reason_code", length = 80) private String reasonCode;
    @Lob @Column private String message;
    @Lob @Column private String metadata;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected TelegramLinkEventJpaEntity() {}

    public TelegramLinkEventJpaEntity(UUID id, UUID companyId, UUID clientId, UUID invitationTokenId, UUID companyUserId, Long telegramUserId, Long telegramChatId, String eventType, String result, String reasonCode, String message, String metadata, Instant createdAt) {
        this.id = id; this.companyId = companyId; this.clientId = clientId; this.invitationTokenId = invitationTokenId; this.companyUserId = companyUserId; this.telegramUserId = telegramUserId; this.telegramChatId = telegramChatId; this.eventType = eventType; this.result = result; this.reasonCode = reasonCode; this.message = message; this.metadata = metadata; this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getEventType() { return eventType; }
    public String getResult() { return result; }
}
