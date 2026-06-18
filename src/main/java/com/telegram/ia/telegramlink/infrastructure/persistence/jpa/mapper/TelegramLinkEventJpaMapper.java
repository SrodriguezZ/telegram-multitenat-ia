package com.telegram.ia.telegramlink.infrastructure.persistence.jpa.mapper;

import com.telegram.ia.telegramlink.domain.model.TelegramLinkEvent;
import com.telegram.ia.telegramlink.domain.valueobject.ClientId;
import com.telegram.ia.telegramlink.domain.valueobject.CompanyId;
import com.telegram.ia.telegramlink.domain.valueobject.CompanyUserId;
import com.telegram.ia.telegramlink.domain.valueobject.InvitationTokenId;
import com.telegram.ia.telegramlink.infrastructure.persistence.jpa.entity.TelegramLinkEventJpaEntity;

public final class TelegramLinkEventJpaMapper {
    private TelegramLinkEventJpaMapper() {}

    public static TelegramLinkEventJpaEntity toEntity(TelegramLinkEvent event) {
        return new TelegramLinkEventJpaEntity(
                event.id().value(),
                event.companyId().map(CompanyId::value).orElse(null),
                event.clientId().map(ClientId::value).orElse(null),
                event.invitationTokenId().map(InvitationTokenId::value).orElse(null),
                event.companyUserId().map(CompanyUserId::value).orElse(null),
                event.telegramUserId().orElse(null),
                event.telegramChatId().orElse(null),
                event.eventType().name(),
                event.result().name(),
                event.reasonCode(),
                event.message(),
                event.metadata(),
                event.createdAt());
    }
}
