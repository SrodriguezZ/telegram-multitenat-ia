package com.telegram.ia.telegramlink.application.command;

import com.telegram.ia.telegramlink.domain.valueobject.ClientId;

public record CreateTelegramInvitationCommand(ClientId clientId) {
    public CreateTelegramInvitationCommand {
        if (clientId == null) throw new IllegalArgumentException("client id is required");
    }
}
