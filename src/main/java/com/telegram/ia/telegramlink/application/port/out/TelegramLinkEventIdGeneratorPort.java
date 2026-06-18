package com.telegram.ia.telegramlink.application.port.out;

import com.telegram.ia.telegramlink.domain.valueobject.TelegramLinkEventId;

public interface TelegramLinkEventIdGeneratorPort {
    TelegramLinkEventId nextId();
}
