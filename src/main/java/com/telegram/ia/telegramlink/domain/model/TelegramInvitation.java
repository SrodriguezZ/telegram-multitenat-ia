package com.telegram.ia.telegramlink.domain.model;

import com.telegram.ia.telegramlink.domain.valueobject.ClientId;
import com.telegram.ia.telegramlink.domain.valueobject.CompanyId;
import com.telegram.ia.telegramlink.domain.valueobject.CompanyUserId;
import com.telegram.ia.telegramlink.domain.valueobject.InvitationTokenId;
import java.time.Instant;
import java.util.Optional;

public record TelegramInvitation(
        InvitationTokenId id,
        CompanyId companyId,
        ClientId clientId,
        CompanyUserId createdByUserId,
        String tokenHash,
        String tokenPrefix,
        TelegramInvitationStatus status,
        Instant expiresAt,
        Optional<Instant> usedAt,
        Optional<Instant> revokedAt,
        Optional<CompanyUserId> revokedByUserId,
        Optional<Long> usedByTelegramUserId,
        Instant createdAt,
        Instant updatedAt) {

    public TelegramInvitation {
        if (tokenHash == null || tokenHash.isBlank()) throw new IllegalArgumentException("token hash is required");
        if (status == null) throw new IllegalArgumentException("invitation status is required");
        usedAt = usedAt == null ? Optional.empty() : usedAt;
        revokedAt = revokedAt == null ? Optional.empty() : revokedAt;
        revokedByUserId = revokedByUserId == null ? Optional.empty() : revokedByUserId;
        usedByTelegramUserId = usedByTelegramUserId == null ? Optional.empty() : usedByTelegramUserId;
    }

    public static TelegramInvitation createPending(
            InvitationTokenId id,
            CompanyId companyId,
            ClientId clientId,
            CompanyUserId createdByUserId,
            String tokenHash,
            String tokenPrefix,
            Instant createdAt,
            Instant expiresAt) {
        return new TelegramInvitation(id, companyId, clientId, createdByUserId, tokenHash, tokenPrefix,
                TelegramInvitationStatus.PENDING, expiresAt, Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), createdAt, createdAt);
    }

    public boolean isPendingAt(Instant now) {
        return status == TelegramInvitationStatus.PENDING && expiresAt.isAfter(now);
    }

    public TelegramInvitation markUsed(TelegramProfileSnapshot profile, Instant usedAt) {
        return new TelegramInvitation(id, companyId, clientId, createdByUserId, tokenHash, tokenPrefix,
                TelegramInvitationStatus.USED, expiresAt, Optional.of(usedAt), revokedAt, revokedByUserId,
                Optional.of(profile.telegramUserId()), createdAt, usedAt);
    }

    public TelegramInvitation revoke(CompanyUserId revokedByUserId, Instant revokedAt) {
        return new TelegramInvitation(id, companyId, clientId, createdByUserId, tokenHash, tokenPrefix,
                TelegramInvitationStatus.REVOKED, expiresAt, usedAt, Optional.of(revokedAt), Optional.of(revokedByUserId),
                usedByTelegramUserId, createdAt, revokedAt);
    }
}
