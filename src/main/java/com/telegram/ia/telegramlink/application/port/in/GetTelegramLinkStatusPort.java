package com.telegram.ia.telegramlink.application.port.in;

import com.telegram.ia.telegramlink.application.query.GetTelegramLinkStatusQuery;
import com.telegram.ia.telegramlink.application.response.TelegramLinkStatusResponse;

public interface GetTelegramLinkStatusPort {
    TelegramLinkStatusResponse execute(GetTelegramLinkStatusQuery query);
}
