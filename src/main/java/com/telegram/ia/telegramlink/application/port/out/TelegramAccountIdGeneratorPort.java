package com.telegram.ia.telegramlink.application.port.out;

import com.telegram.ia.telegramlink.domain.valueobject.TelegramAccountId;

public interface TelegramAccountIdGeneratorPort {
    TelegramAccountId nextId();
}
