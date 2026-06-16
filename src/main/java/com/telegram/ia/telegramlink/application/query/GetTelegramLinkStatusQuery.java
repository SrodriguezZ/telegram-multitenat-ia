package com.telegram.ia.telegramlink.application.query;

import com.telegram.ia.telegramlink.domain.valueobject.ClientId;

public record GetTelegramLinkStatusQuery(ClientId clientId) {
    public GetTelegramLinkStatusQuery {
        if (clientId == null) throw new IllegalArgumentException("client id is required");
    }
}
