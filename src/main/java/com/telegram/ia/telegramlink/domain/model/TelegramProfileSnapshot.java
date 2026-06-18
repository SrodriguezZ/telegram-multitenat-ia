package com.telegram.ia.telegramlink.domain.model;

public record TelegramProfileSnapshot(
        long telegramUserId,
        Long telegramChatId,
        String telegramUsername,
        String telegramFirstName,
        String telegramLastName) {}
