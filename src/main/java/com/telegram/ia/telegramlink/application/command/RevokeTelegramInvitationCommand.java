package com.telegram.ia.telegramlink.application.command;

import com.telegram.ia.telegramlink.domain.valueobject.InvitationTokenId;

public record RevokeTelegramInvitationCommand(InvitationTokenId invitationId) {
    public RevokeTelegramInvitationCommand {
        if (invitationId == null) throw new IllegalArgumentException("invitation id is required");
    }
}
