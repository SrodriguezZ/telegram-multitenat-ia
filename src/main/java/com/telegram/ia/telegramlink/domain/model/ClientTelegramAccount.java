package com.telegram.ia.telegramlink.domain.model;

import com.telegram.ia.telegramlink.domain.valueobject.ClientId;
import com.telegram.ia.telegramlink.domain.valueobject.CompanyId;
import com.telegram.ia.telegramlink.domain.valueobject.InvitationTokenId;
import com.telegram.ia.telegramlink.domain.valueobject.TelegramAccountId;
import java.time.Instant;
import java.util.Optional;

public record ClientTelegramAccount(
        TelegramAccountId id,
        CompanyId companyId,
        ClientId clientId,
        long telegramUserId,
        Long telegramChatId,
        String telegramUsername,
        String telegramFirstName,
        String telegramLastName,
        Optional<InvitationTokenId> linkedByInvitationTokenId,
        TelegramAccountStatus status,
        Instant linkedAt,
        Optional<Instant> unlinkedAt,
        Instant createdAt,
        Instant updatedAt) {}
