package com.telegram.ia.telegramlink.infrastructure.web.bot;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record ConfirmTelegramLinkHttpRequest(
        @NotBlank String token,
        @Positive long telegramUserId,
        Long telegramChatId,
        String telegramUsername,
        String telegramFirstName,
        String telegramLastName) {}
