package com.telegram.ia.telegramlink.domain.valueobject;

import java.util.UUID;

public record ClientId(UUID value) {
    public ClientId {
        if (value == null) throw new IllegalArgumentException("client id is required");
    }
}
