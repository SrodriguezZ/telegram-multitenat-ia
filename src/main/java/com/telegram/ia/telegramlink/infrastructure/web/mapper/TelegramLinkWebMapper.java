package com.telegram.ia.telegramlink.infrastructure.web.mapper;

import com.telegram.ia.telegramlink.application.response.ConfirmTelegramLinkResponse;
import com.telegram.ia.telegramlink.application.response.CreateTelegramInvitationResponse;
import com.telegram.ia.telegramlink.application.response.RevokeTelegramInvitationResponse;
import com.telegram.ia.telegramlink.application.response.TelegramLinkStatusResponse;
import com.telegram.ia.telegramlink.application.response.TelegramLinkTokenValidationResponse;
import com.telegram.ia.telegramlink.domain.valueobject.ClientId;
import com.telegram.ia.telegramlink.domain.valueobject.InvitationTokenId;
import com.telegram.ia.telegramlink.domain.valueobject.TelegramAccountId;
import com.telegram.ia.telegramlink.infrastructure.web.admin.TelegramInvitationCreatedHttpResponse;
import com.telegram.ia.telegramlink.infrastructure.web.admin.TelegramInvitationRevokedHttpResponse;
import com.telegram.ia.telegramlink.infrastructure.web.admin.TelegramLinkStatusHttpResponse;
import com.telegram.ia.telegramlink.infrastructure.web.bot.ConfirmTelegramLinkHttpResponse;
import com.telegram.ia.telegramlink.infrastructure.web.bot.TelegramLinkTokenValidationHttpResponse;

public final class TelegramLinkWebMapper {
    private TelegramLinkWebMapper() {}

    public static TelegramInvitationCreatedHttpResponse toHttp(CreateTelegramInvitationResponse response) {
        return new TelegramInvitationCreatedHttpResponse(
                response.invitationId().value(), response.status().name(), response.link(), response.expiresAt(), response.tokenPrefix());
    }

    public static TelegramInvitationRevokedHttpResponse toHttp(RevokeTelegramInvitationResponse response) {
        return new TelegramInvitationRevokedHttpResponse(response.invitationId().value(), response.status().name(), response.revokedAt());
    }

    public static TelegramLinkStatusHttpResponse toHttp(TelegramLinkStatusResponse response) {
        return new TelegramLinkStatusHttpResponse(
                response.clientId().value(),
                response.status().name(),
                response.invitationId().map(InvitationTokenId::value).orElse(null),
                response.invitationExpiresAt().orElse(null),
                response.telegramUserId().orElse(null),
                response.telegramUsername().orElse(null),
                response.linkedAt().orElse(null));
    }

    public static TelegramLinkTokenValidationHttpResponse toHttp(TelegramLinkTokenValidationResponse response) {
        return new TelegramLinkTokenValidationHttpResponse(
                response.status().name(),
                response.confirmationRequired(),
                response.errorCode().orElse(null),
                response.clientId().map(ClientId::value).orElse(null),
                response.clientFullName().orElse(null),
                response.expiresAt().orElse(null));
    }

    public static ConfirmTelegramLinkHttpResponse toHttp(ConfirmTelegramLinkResponse response) {
        return new ConfirmTelegramLinkHttpResponse(
                response.status().name(),
                response.errorCode().orElse(null),
                response.clientId().map(ClientId::value).orElse(null),
                response.telegramAccountId().map(TelegramAccountId::value).orElse(null),
                response.telegramUserId().orElse(null),
                response.linkedAt().orElse(null));
    }
}
