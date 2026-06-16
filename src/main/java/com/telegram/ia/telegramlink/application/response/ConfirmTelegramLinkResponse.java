package com.telegram.ia.telegramlink.application.response;

import com.telegram.ia.telegramlink.domain.valueobject.ClientId;
import com.telegram.ia.telegramlink.domain.valueobject.TelegramAccountId;
import java.time.Instant;
import java.util.Optional;

public record ConfirmTelegramLinkResponse(
        TelegramLinkConfirmationStatus status,
        Optional<String> errorCode,
        Optional<ClientId> clientId,
        Optional<TelegramAccountId> telegramAccountId,
        Optional<Long> telegramUserId,
        Optional<Instant> linkedAt) {

    public static ConfirmTelegramLinkResponse confirmed(
            ClientId clientId,
            TelegramAccountId telegramAccountId,
            long telegramUserId,
            Instant linkedAt) {
        return new ConfirmTelegramLinkResponse(
                TelegramLinkConfirmationStatus.CONFIRMED,
                Optional.empty(),
                Optional.of(clientId),
                Optional.of(telegramAccountId),
                Optional.of(telegramUserId),
                Optional.of(linkedAt));
    }

    public static ConfirmTelegramLinkResponse invalid(String errorCode) {
        return new ConfirmTelegramLinkResponse(
                TelegramLinkConfirmationStatus.INVALID,
                Optional.of(errorCode),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }
}
