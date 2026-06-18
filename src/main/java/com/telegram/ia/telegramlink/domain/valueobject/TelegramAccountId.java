package com.telegram.ia.telegramlink.domain.valueobject;

import java.util.UUID;

public record TelegramAccountId(UUID value) {
    public TelegramAccountId {
        if (value == null) throw new IllegalArgumentException("telegram account id is required");
    }
}
