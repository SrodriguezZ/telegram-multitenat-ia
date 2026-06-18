package com.telegram.ia.telegramlink.infrastructure.web.admin;

import java.time.Instant;
import java.util.UUID;

public record TelegramLinkStatusHttpResponse(
        UUID clientId,
        String status,
        UUID invitationId,
        Instant invitationExpiresAt,
        Long telegramUserId,
        String telegramUsername,
        Instant linkedAt) {}
