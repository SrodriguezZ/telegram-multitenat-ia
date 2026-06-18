package com.telegram.ia.telegramlink.infrastructure.web.bot;

import java.time.Instant;
import java.util.UUID;

public record ConfirmTelegramLinkHttpResponse(
        String status,
        String errorCode,
        UUID clientId,
        UUID telegramAccountId,
        Long telegramUserId,
        Instant linkedAt) {}
