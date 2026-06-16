package com.telegram.ia.telegramlink.domain.model;

import com.telegram.ia.telegramlink.domain.valueobject.ClientId;
import com.telegram.ia.telegramlink.domain.valueobject.CompanyId;
import com.telegram.ia.telegramlink.domain.valueobject.CompanyUserId;
import com.telegram.ia.telegramlink.domain.valueobject.InvitationTokenId;
import com.telegram.ia.telegramlink.domain.valueobject.TelegramLinkEventId;
import java.time.Instant;
import java.util.Optional;

public record TelegramLinkEvent(
        TelegramLinkEventId id,
        Optional<CompanyId> companyId,
        Optional<ClientId> clientId,
        Optional<InvitationTokenId> invitationTokenId,
        Optional<CompanyUserId> companyUserId,
        Optional<Long> telegramUserId,
        Optional<Long> telegramChatId,
        TelegramLinkEventType eventType,
        TelegramLinkEventResult result,
        String reasonCode,
        String message,
        String metadata,
        Instant createdAt) {}
