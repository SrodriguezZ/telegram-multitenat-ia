package com.telegram.ia.telegramlink.infrastructure.persistence.jpa.mapper;

import com.telegram.ia.telegramlink.domain.model.TelegramInvitation;
import com.telegram.ia.telegramlink.domain.model.TelegramInvitationStatus;
import com.telegram.ia.telegramlink.domain.valueobject.ClientId;
import com.telegram.ia.telegramlink.domain.valueobject.CompanyId;
import com.telegram.ia.telegramlink.domain.valueobject.CompanyUserId;
import com.telegram.ia.telegramlink.domain.valueobject.InvitationTokenId;
import com.telegram.ia.telegramlink.infrastructure.persistence.jpa.entity.TelegramInvitationTokenJpaEntity;
import java.util.Optional;

public final class TelegramInvitationJpaMapper {
    private TelegramInvitationJpaMapper() {}

    public static TelegramInvitationTokenJpaEntity toEntity(TelegramInvitation invitation) {
        return new TelegramInvitationTokenJpaEntity(
                invitation.id().value(), invitation.companyId().value(), invitation.clientId().value(),
                invitation.createdByUserId().value(), invitation.tokenHash(), invitation.tokenPrefix(),
                invitation.status().name(), invitation.expiresAt(), invitation.usedAt().orElse(null),
                invitation.revokedAt().orElse(null), invitation.revokedByUserId().map(CompanyUserId::value).orElse(null),
                invitation.usedByTelegramUserId().orElse(null), invitation.createdAt(), invitation.updatedAt());
    }

    public static TelegramInvitation toDomain(TelegramInvitationTokenJpaEntity entity) {
        return new TelegramInvitation(
                new InvitationTokenId(entity.getId()), new CompanyId(entity.getCompanyId()), new ClientId(entity.getClientId()),
                new CompanyUserId(entity.getCreatedByUserId()), entity.getTokenHash(), entity.getTokenPrefix(),
                TelegramInvitationStatus.valueOf(entity.getStatus()), entity.getExpiresAt(), Optional.ofNullable(entity.getUsedAt()),
                Optional.ofNullable(entity.getRevokedAt()), Optional.ofNullable(entity.getRevokedByUserId()).map(CompanyUserId::new),
                Optional.ofNullable(entity.getUsedByTelegramUserId()), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
