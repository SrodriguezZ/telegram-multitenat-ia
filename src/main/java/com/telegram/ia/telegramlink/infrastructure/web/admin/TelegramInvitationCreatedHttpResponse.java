package com.telegram.ia.telegramlink.infrastructure.web.admin;

import java.time.Instant;
import java.util.UUID;

public record TelegramInvitationCreatedHttpResponse(
        UUID invitationId,
        String status,
        String link,
        Instant expiresAt,
        String tokenPrefix) {}
