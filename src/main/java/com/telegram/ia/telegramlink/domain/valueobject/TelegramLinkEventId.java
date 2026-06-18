package com.telegram.ia.telegramlink.domain.valueobject;

import java.util.UUID;

public record TelegramLinkEventId(UUID value) {
    public TelegramLinkEventId {
        if (value == null) throw new IllegalArgumentException("telegram link event id is required");
    }
}
