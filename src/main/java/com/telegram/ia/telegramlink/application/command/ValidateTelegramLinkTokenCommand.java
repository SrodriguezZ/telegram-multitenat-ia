package com.telegram.ia.telegramlink.application.command;

public record ValidateTelegramLinkTokenCommand(String rawToken, long telegramUserId) {}
