package com.telegram.ia.telegramlink.infrastructure.persistence.jpa.mapper;

import com.telegram.ia.telegramlink.domain.model.ClientTelegramAccount;
import com.telegram.ia.telegramlink.domain.model.TelegramAccountStatus;
import com.telegram.ia.telegramlink.domain.valueobject.ClientId;
import com.telegram.ia.telegramlink.domain.valueobject.CompanyId;
import com.telegram.ia.telegramlink.domain.valueobject.InvitationTokenId;
import com.telegram.ia.telegramlink.domain.valueobject.TelegramAccountId;
import com.telegram.ia.telegramlink.infrastructure.persistence.jpa.entity.ClientTelegramAccountJpaEntity;
import java.util.Optional;

public final class ClientTelegramAccountJpaMapper {
    private ClientTelegramAccountJpaMapper() {}

    public static ClientTelegramAccountJpaEntity toEntity(ClientTelegramAccount account) {
        return new ClientTelegramAccountJpaEntity(
                account.id().value(),
                account.companyId().value(),
                account.clientId().value(),
                account.telegramUserId(),
                account.telegramChatId(),
                account.telegramUsername(),
                account.telegramFirstName(),
                account.telegramLastName(),
                account.linkedByInvitationTokenId().map(InvitationTokenId::value).orElse(null),
                account.status().name(),
                account.linkedAt(),
                account.unlinkedAt().orElse(null),
                account.createdAt(),
                account.updatedAt());
    }

    public static ClientTelegramAccount toDomain(ClientTelegramAccountJpaEntity entity) {
        return new ClientTelegramAccount(
                new TelegramAccountId(entity.getId()),
                new CompanyId(entity.getCompanyId()),
                new ClientId(entity.getClientId()),
                entity.getTelegramUserId(),
                entity.getTelegramChatId(),
                entity.getTelegramUsername(),
                entity.getTelegramFirstName(),
                entity.getTelegramLastName(),
                Optional.ofNullable(entity.getLinkedByInvitationTokenId()).map(InvitationTokenId::new),
                TelegramAccountStatus.valueOf(entity.getStatus()),
                entity.getLinkedAt(),
                Optional.ofNullable(entity.getUnlinkedAt()),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
