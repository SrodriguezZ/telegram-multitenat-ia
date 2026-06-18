package com.telegram.ia.telegramlink.infrastructure.web.bot;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record ValidateTelegramLinkTokenHttpRequest(@NotBlank String token, @Positive long telegramUserId) {}
