package com.telegram.ia.telegramlink.application.port.in;

import com.telegram.ia.telegramlink.application.command.RevokeTelegramInvitationCommand;
import com.telegram.ia.telegramlink.application.response.RevokeTelegramInvitationResponse;

public interface RevokeTelegramInvitationPort {
    RevokeTelegramInvitationResponse execute(RevokeTelegramInvitationCommand command);
}
