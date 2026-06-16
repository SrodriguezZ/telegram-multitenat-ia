package com.telegram.ia.telegramlink.application.response;

import com.telegram.ia.telegramlink.domain.valueobject.ClientId;
import java.time.Instant;
import java.util.Optional;

public record TelegramLinkTokenValidationResponse(
        TelegramLinkTokenValidationStatus status,
        boolean confirmationRequired,
        Optional<String> errorCode,
        Optional<ClientId> clientId,
        Optional<String> clientFullName,
        Optional<Instant> expiresAt) {

    public static TelegramLinkTokenValidationResponse valid(ClientId clientId, String clientFullName, Instant expiresAt) {
        return new TelegramLinkTokenValidationResponse(
                TelegramLinkTokenValidationStatus.VALID,
                true,
                Optional.empty(),
                Optional.of(clientId),
                Optional.ofNullable(clientFullName),
                Optional.of(expiresAt));
    }

    public static TelegramLinkTokenValidationResponse invalid(String errorCode) {
        return new TelegramLinkTokenValidationResponse(
                TelegramLinkTokenValidationStatus.INVALID,
                false,
                Optional.of(errorCode),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }
}
