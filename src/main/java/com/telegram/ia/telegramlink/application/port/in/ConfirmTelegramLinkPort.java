package com.telegram.ia.telegramlink.application.port.in;

import com.telegram.ia.telegramlink.application.command.ConfirmTelegramLinkCommand;
import com.telegram.ia.telegramlink.application.response.ConfirmTelegramLinkResponse;

public interface ConfirmTelegramLinkPort {
    ConfirmTelegramLinkResponse execute(ConfirmTelegramLinkCommand command);
}
