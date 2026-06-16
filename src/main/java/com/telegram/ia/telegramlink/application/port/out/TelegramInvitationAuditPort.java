package com.telegram.ia.telegramlink.application.port.out;

import com.telegram.ia.telegramlink.domain.model.TelegramLinkEvent;

public interface TelegramInvitationAuditPort {
    void record(TelegramLinkEvent event);
}
