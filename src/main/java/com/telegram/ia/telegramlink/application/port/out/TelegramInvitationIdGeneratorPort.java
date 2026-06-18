package com.telegram.ia.telegramlink.application.port.out;

import com.telegram.ia.telegramlink.domain.valueobject.InvitationTokenId;

public interface TelegramInvitationIdGeneratorPort {
    InvitationTokenId nextId();
}
