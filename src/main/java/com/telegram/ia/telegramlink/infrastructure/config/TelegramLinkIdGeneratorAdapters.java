package com.telegram.ia.telegramlink.infrastructure.config;

import com.telegram.ia.telegramlink.application.port.out.TelegramAccountIdGeneratorPort;
import com.telegram.ia.telegramlink.application.port.out.TelegramInvitationIdGeneratorPort;
import com.telegram.ia.telegramlink.application.port.out.TelegramLinkEventIdGeneratorPort;
import com.telegram.ia.telegramlink.domain.valueobject.InvitationTokenId;
import com.telegram.ia.telegramlink.domain.valueobject.TelegramAccountId;
import com.telegram.ia.telegramlink.domain.valueobject.TelegramLinkEventId;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TelegramLinkIdGeneratorAdapters {
    @Bean
    TelegramInvitationIdGeneratorPort telegramInvitationIdGeneratorPort() {
        return () -> new InvitationTokenId(UUID.randomUUID());
    }

    @Bean
    TelegramLinkEventIdGeneratorPort telegramLinkEventIdGeneratorPort() {
        return () -> new TelegramLinkEventId(UUID.randomUUID());
    }

    @Bean
    TelegramAccountIdGeneratorPort telegramAccountIdGeneratorPort() {
        return () -> new TelegramAccountId(UUID.randomUUID());
    }
}
