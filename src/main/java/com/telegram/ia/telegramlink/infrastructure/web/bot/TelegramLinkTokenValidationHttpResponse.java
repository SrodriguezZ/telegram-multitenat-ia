package com.telegram.ia.telegramlink.infrastructure.web.bot;

import java.time.Instant;
import java.util.UUID;

public record TelegramLinkTokenValidationHttpResponse(
        String status,
        boolean confirmationRequired,
        String errorCode,
        UUID clientId,
        String clientFullName,
        Instant expiresAt) {}
