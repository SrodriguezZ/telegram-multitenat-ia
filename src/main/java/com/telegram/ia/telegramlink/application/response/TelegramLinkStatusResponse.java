package com.telegram.ia.telegramlink.application.response;

import com.telegram.ia.telegramlink.domain.valueobject.ClientId;
import com.telegram.ia.telegramlink.domain.valueobject.InvitationTokenId;
import java.time.Instant;
import java.util.Optional;

public record TelegramLinkStatusResponse(
        ClientId clientId,
        TelegramLinkStatus status,
        Optional<InvitationTokenId> invitationId,
        Optional<Instant> invitationExpiresAt,
        Optional<Long> telegramUserId,
        Optional<String> telegramUsername,
        Optional<Instant> linkedAt) {

    public TelegramLinkStatusResponse {
        invitationId = invitationId == null ? Optional.empty() : invitationId;
        invitationExpiresAt = invitationExpiresAt == null ? Optional.empty() : invitationExpiresAt;
        telegramUserId = telegramUserId == null ? Optional.empty() : telegramUserId;
        telegramUsername = telegramUsername == null ? Optional.empty() : telegramUsername;
        linkedAt = linkedAt == null ? Optional.empty() : linkedAt;
    }

    public static TelegramLinkStatusResponse notLinked(ClientId clientId) {
        return new TelegramLinkStatusResponse(clientId, TelegramLinkStatus.NOT_LINKED,
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }
}
