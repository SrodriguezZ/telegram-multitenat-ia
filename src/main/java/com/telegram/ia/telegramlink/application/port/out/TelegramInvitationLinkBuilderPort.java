package com.telegram.ia.telegramlink.application.port.out;

public interface TelegramInvitationLinkBuilderPort {
    String buildLink(String rawToken);
}
