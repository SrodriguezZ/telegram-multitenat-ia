package com.telegram.ia.telegramlink.application.port.in;

import com.telegram.ia.telegramlink.application.command.ValidateTelegramLinkTokenCommand;
import com.telegram.ia.telegramlink.application.response.TelegramLinkTokenValidationResponse;

public interface ValidateTelegramLinkTokenPort {
    TelegramLinkTokenValidationResponse execute(ValidateTelegramLinkTokenCommand command);
}
