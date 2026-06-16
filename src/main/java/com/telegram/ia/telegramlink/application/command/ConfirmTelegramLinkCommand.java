package com.telegram.ia.telegramlink.application.command;

import com.telegram.ia.telegramlink.domain.model.TelegramProfileSnapshot;

public record ConfirmTelegramLinkCommand(String rawToken, TelegramProfileSnapshot telegramProfile) {}
