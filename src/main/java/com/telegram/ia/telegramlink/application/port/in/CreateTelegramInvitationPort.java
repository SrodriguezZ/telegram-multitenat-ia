package com.telegram.ia.telegramlink.application.port.in;

import com.telegram.ia.telegramlink.application.command.CreateTelegramInvitationCommand;
import com.telegram.ia.telegramlink.application.response.CreateTelegramInvitationResponse;

public interface CreateTelegramInvitationPort {
    CreateTelegramInvitationResponse execute(CreateTelegramInvitationCommand command);
}
