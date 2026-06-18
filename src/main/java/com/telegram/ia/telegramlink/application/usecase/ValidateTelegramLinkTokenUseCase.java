package com.telegram.ia.telegramlink.application.usecase;

import com.telegram.ia.telegramlink.application.command.ValidateTelegramLinkTokenCommand;
import com.telegram.ia.telegramlink.application.port.in.ValidateTelegramLinkTokenPort;
import com.telegram.ia.telegramlink.application.port.out.ClientRepositoryPort;
import com.telegram.ia.telegramlink.application.port.out.ClientTelegramAccountRepositoryPort;
import com.telegram.ia.telegramlink.application.port.out.ClockPort;
import com.telegram.ia.telegramlink.application.port.out.TelegramInvitationAuditPort;
import com.telegram.ia.telegramlink.application.port.out.TelegramInvitationRepositoryPort;
import com.telegram.ia.telegramlink.application.port.out.TelegramLinkEventIdGeneratorPort;
import com.telegram.ia.telegramlink.application.port.out.TokenHashingPort;
import com.telegram.ia.telegramlink.application.response.TelegramLinkTokenValidationResponse;
import com.telegram.ia.telegramlink.domain.model.Client;
import com.telegram.ia.telegramlink.domain.model.TelegramInvitation;
import com.telegram.ia.telegramlink.domain.model.TelegramInvitationStatus;
import com.telegram.ia.telegramlink.domain.model.TelegramLinkEvent;
import com.telegram.ia.telegramlink.domain.model.TelegramLinkEventResult;
import com.telegram.ia.telegramlink.domain.model.TelegramLinkEventType;
import com.telegram.ia.telegramlink.domain.valueobject.ClientId;
import com.telegram.ia.telegramlink.domain.valueobject.CompanyId;
import com.telegram.ia.telegramlink.domain.valueobject.InvitationTokenId;
import java.time.Instant;
import java.util.Optional;

public class ValidateTelegramLinkTokenUseCase implements ValidateTelegramLinkTokenPort {
    private final TelegramInvitationRepositoryPort invitationRepository;
    private final ClientRepositoryPort clientRepository;
    private final ClientTelegramAccountRepositoryPort accountRepository;
    private final TokenHashingPort tokenHashing;
    private final TelegramLinkEventIdGeneratorPort eventIdGenerator;
    private final TelegramInvitationAuditPort auditPort;
    private final ClockPort clock;

    public ValidateTelegramLinkTokenUseCase(
            TelegramInvitationRepositoryPort invitationRepository,
            ClientRepositoryPort clientRepository,
            ClientTelegramAccountRepositoryPort accountRepository,
            TokenHashingPort tokenHashing,
            TelegramLinkEventIdGeneratorPort eventIdGenerator,
            TelegramInvitationAuditPort auditPort,
            ClockPort clock) {
        this.invitationRepository = invitationRepository;
        this.clientRepository = clientRepository;
        this.accountRepository = accountRepository;
        this.tokenHashing = tokenHashing;
        this.eventIdGenerator = eventIdGenerator;
        this.auditPort = auditPort;
        this.clock = clock;
    }

    @Override
    public TelegramLinkTokenValidationResponse execute(ValidateTelegramLinkTokenCommand command) {
        Instant now = clock.now();
        if (command.rawToken() == null || command.rawToken().isBlank()) {
            auditFailure(Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(command.telegramUserId()), Optional.empty(),
                    TelegramLinkEventType.TOKEN_VALIDATION_FAILED, "INVALID_TOKEN", "Token is invalid");
            return TelegramLinkTokenValidationResponse.invalid("INVALID_TOKEN");
        }

        Optional<TelegramInvitation> invitation = invitationRepository.findByTokenHash(tokenHashing.hash(command.rawToken()));
        if (invitation.isEmpty()) {
            auditFailure(Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(command.telegramUserId()), Optional.empty(),
                    TelegramLinkEventType.TOKEN_VALIDATION_FAILED, "INVALID_TOKEN", "Token is invalid");
            return TelegramLinkTokenValidationResponse.invalid("INVALID_TOKEN");
        }

        TelegramInvitation current = invitation.get();
        Optional<String> invalidReason = invalidInvitationReason(current, now);
        if (invalidReason.isPresent()) {
            auditFailure(Optional.of(current.companyId()), Optional.of(current.clientId()), Optional.of(current.id()), Optional.of(command.telegramUserId()), Optional.of(current.tokenPrefix()),
                    TelegramLinkEventType.TOKEN_VALIDATION_FAILED, invalidReason.get(), "Token cannot be used");
            return TelegramLinkTokenValidationResponse.invalid(invalidReason.get());
        }

        Optional<Client> client = clientRepository.findById(current.clientId())
                .filter(found -> found.companyId().equals(current.companyId()))
                .filter(Client::active);
        if (client.isEmpty()) {
            auditFailure(Optional.of(current.companyId()), Optional.of(current.clientId()), Optional.of(current.id()), Optional.of(command.telegramUserId()), Optional.of(current.tokenPrefix()),
                    TelegramLinkEventType.TOKEN_VALIDATION_FAILED, "CLIENT_NOT_AVAILABLE", "Client is not available");
            return TelegramLinkTokenValidationResponse.invalid("CLIENT_NOT_AVAILABLE");
        }
        if (accountRepository.existsActiveByCompanyIdAndClientId(current.companyId(), current.clientId())) {
            auditFailure(Optional.of(current.companyId()), Optional.of(current.clientId()), Optional.of(current.id()), Optional.of(command.telegramUserId()), Optional.of(current.tokenPrefix()),
                    TelegramLinkEventType.TOKEN_VALIDATION_FAILED, "CLIENT_ALREADY_LINKED", "Client already has an active Telegram link");
            return TelegramLinkTokenValidationResponse.invalid("CLIENT_ALREADY_LINKED");
        }
        if (accountRepository.existsActiveByCompanyIdAndTelegramUserId(current.companyId(), command.telegramUserId())) {
            auditFailure(Optional.of(current.companyId()), Optional.of(current.clientId()), Optional.of(current.id()), Optional.of(command.telegramUserId()), Optional.of(current.tokenPrefix()),
                    TelegramLinkEventType.DUPLICATE_TELEGRAM_ACCOUNT_DETECTED, "TELEGRAM_ACCOUNT_ALREADY_LINKED", "Telegram account is already linked");
            return TelegramLinkTokenValidationResponse.invalid("TELEGRAM_ACCOUNT_ALREADY_LINKED");
        }

        Client activeClient = client.get();
        auditSuccess(current.companyId(), activeClient.id(), current.id(), command.telegramUserId(), current.tokenPrefix());
        return TelegramLinkTokenValidationResponse.valid(activeClient.id(), activeClient.fullName(), current.expiresAt());
    }

    private Optional<String> invalidInvitationReason(TelegramInvitation invitation, Instant now) {
        if (invitation.status() == TelegramInvitationStatus.USED) return Optional.of("INVITATION_ALREADY_USED");
        if (invitation.status() == TelegramInvitationStatus.REVOKED) return Optional.of("INVITATION_REVOKED");
        if (!invitation.expiresAt().isAfter(now)) return Optional.of("INVITATION_EXPIRED");
        if (invitation.status() != TelegramInvitationStatus.PENDING) return Optional.of("INVALID_TOKEN");
        return Optional.empty();
    }

    private void auditSuccess(CompanyId companyId, ClientId clientId, InvitationTokenId invitationId, long telegramUserId, String tokenPrefix) {
        auditPort.record(event(Optional.of(companyId), Optional.of(clientId), Optional.of(invitationId), Optional.of(telegramUserId),
                TelegramLinkEventType.TOKEN_VALIDATION_STARTED, TelegramLinkEventResult.SUCCESS, "TOKEN_VALID", "Token preview is valid", tokenMetadata(tokenPrefix)));
    }

    private void auditFailure(
            Optional<CompanyId> companyId,
            Optional<ClientId> clientId,
            Optional<InvitationTokenId> invitationId,
            Optional<Long> telegramUserId,
            Optional<String> tokenPrefix,
            TelegramLinkEventType eventType,
            String reasonCode,
            String message) {
        auditPort.record(event(companyId, clientId, invitationId, telegramUserId, eventType, TelegramLinkEventResult.FAILURE, reasonCode, message,
                tokenPrefix.map(this::tokenMetadata).orElse("{}")));
    }

    private TelegramLinkEvent event(
            Optional<CompanyId> companyId,
            Optional<ClientId> clientId,
            Optional<InvitationTokenId> invitationId,
            Optional<Long> telegramUserId,
            TelegramLinkEventType eventType,
            TelegramLinkEventResult result,
            String reasonCode,
            String message,
            String metadata) {
        return new TelegramLinkEvent(eventIdGenerator.nextId(), companyId, clientId, invitationId, Optional.empty(), telegramUserId, Optional.empty(),
                eventType, result, reasonCode, message, metadata, clock.now());
    }

    private String tokenMetadata(String tokenPrefix) {
        return "{\"tokenPrefix\":\"" + tokenPrefix + "\"}";
    }
}
