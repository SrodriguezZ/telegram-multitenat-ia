package com.telegram.ia.telegramlink.infrastructure.config;

import com.telegram.ia.telegramlink.application.port.out.TelegramInvitationLinkBuilderPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TelegramInvitationLinkBuilderAdapter implements TelegramInvitationLinkBuilderPort {
    private final String botUsername;

    public TelegramInvitationLinkBuilderAdapter(@Value("${telegram-link.bot.username}") String botUsername) {
        if (botUsername == null || botUsername.isBlank()) {
            throw new IllegalArgumentException("telegram-link bot username is required");
        }
        this.botUsername = botUsername;
    }

    @Override
    public String buildLink(String rawToken) {
        return "https://t.me/" + botUsername + "?start=" + rawToken;
    }
}
