package com.telegram.ia.telegramlink.application.response;

import com.telegram.ia.telegramlink.domain.model.TelegramInvitationStatus;
import com.telegram.ia.telegramlink.domain.valueobject.InvitationTokenId;
import java.time.Instant;

public record CreateTelegramInvitationResponse(
        InvitationTokenId invitationId,
        TelegramInvitationStatus status,
        String link,
        Instant expiresAt,
        String tokenPrefix) {}
