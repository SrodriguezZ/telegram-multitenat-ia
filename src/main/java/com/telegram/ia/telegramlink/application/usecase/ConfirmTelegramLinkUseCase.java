package com.telegram.ia.telegramlink.application.usecase;

import com.telegram.ia.telegramlink.application.command.ConfirmTelegramLinkCommand;
import com.telegram.ia.telegramlink.application.port.in.ConfirmTelegramLinkPort;
import com.telegram.ia.telegramlink.application.port.out.ClientRepositoryPort;
import com.telegram.ia.telegramlink.application.port.out.ClientTelegramAccountRepositoryPort;
import com.telegram.ia.telegramlink.application.port.out.ClockPort;
import com.telegram.ia.telegramlink.application.port.out.TelegramAccountIdGeneratorPort;
import com.telegram.ia.telegramlink.application.port.out.TelegramInvitationAuditPort;
import com.telegram.ia.telegramlink.application.port.out.TelegramInvitationRepositoryPort;
import com.telegram.ia.telegramlink.application.port.out.TelegramLinkEventIdGeneratorPort;
import com.telegram.ia.telegramlink.application.port.out.TokenHashingPort;
import com.telegram.ia.telegramlink.application.port.out.TransactionRunnerPort;
import com.telegram.ia.telegramlink.application.response.ConfirmTelegramLinkResponse;
import com.telegram.ia.telegramlink.domain.model.Client;
import com.telegram.ia.telegramlink.domain.model.ClientTelegramAccount;
import com.telegram.ia.telegramlink.domain.model.TelegramAccountStatus;
import com.telegram.ia.telegramlink.domain.model.TelegramInvitation;
import com.telegram.ia.telegramlink.domain.model.TelegramInvitationStatus;
import com.telegram.ia.telegramlink.domain.model.TelegramLinkEvent;
import com.telegram.ia.telegramlink.domain.model.TelegramLinkEventResult;
import com.telegram.ia.telegramlink.domain.model.TelegramLinkEventType;
import com.telegram.ia.telegramlink.domain.model.TelegramProfileSnapshot;
import com.telegram.ia.telegramlink.domain.valueobject.ClientId;
import com.telegram.ia.telegramlink.domain.valueobject.CompanyId;
import com.telegram.ia.telegramlink.domain.valueobject.InvitationTokenId;
import java.time.Instant;
import java.util.Optional;

public class ConfirmTelegramLinkUseCase implements ConfirmTelegramLinkPort {
    private final TelegramInvitationRepositoryPort invitationRepository;
    private final ClientRepositoryPort clientRepository;
    private final ClientTelegramAccountRepositoryPort accountRepository;
    private final TokenHashingPort tokenHashing;
    private final TelegramAccountIdGeneratorPort accountIdGenerator;
    private final TelegramLinkEventIdGeneratorPort eventIdGenerator;
    private final TelegramInvitationAuditPort auditPort;
    private final ClockPort clock;
    private final TransactionRunnerPort transactionRunner;

    public ConfirmTelegramLinkUseCase(
            TelegramInvitationRepositoryPort invitationRepository,
            ClientRepositoryPort clientRepository,
            ClientTelegramAccountRepositoryPort accountRepository,
            TokenHashingPort tokenHashing,
            TelegramAccountIdGeneratorPort accountIdGenerator,
            TelegramLinkEventIdGeneratorPort eventIdGenerator,
            TelegramInvitationAuditPort auditPort,
            ClockPort clock,
            TransactionRunnerPort transactionRunner) {
        this.invitationRepository = invitationRepository;
        this.clientRepository = clientRepository;
        this.accountRepository = accountRepository;
        this.tokenHashing = tokenHashing;
        this.accountIdGenerator = accountIdGenerator;
        this.eventIdGenerator = eventIdGenerator;
        this.auditPort = auditPort;
        this.clock = clock;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public ConfirmTelegramLinkResponse execute(ConfirmTelegramLinkCommand command) {
        return transactionRunner.inWriteTransaction(() -> confirm(command));
    }

    private ConfirmTelegramLinkResponse confirm(ConfirmTelegramLinkCommand command) {
        Instant now = clock.now();
        TelegramProfileSnapshot profile = command.telegramProfile();
        if (command.rawToken() == null || command.rawToken().isBlank() || profile == null) {
            auditFailure(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), "INVALID_TOKEN", "Token is invalid");
            return ConfirmTelegramLinkResponse.invalid("INVALID_TOKEN");
        }

        Optional<TelegramInvitation> lockedInvitation = invitationRepository.findByTokenHashForUpdate(tokenHashing.hash(command.rawToken()));
        if (lockedInvitation.isEmpty()) {
            auditFailure(Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(profile.telegramUserId()), Optional.empty(), "INVALID_TOKEN", "Token is invalid");
            return ConfirmTelegramLinkResponse.invalid("INVALID_TOKEN");
        }

        TelegramInvitation invitation = lockedInvitation.get();
        Optional<String> invalidReason = invalidInvitationReason(invitation, now);
        if (invalidReason.isPresent()) {
            auditFailure(Optional.of(invitation.companyId()), Optional.of(invitation.clientId()), Optional.of(invitation.id()), Optional.of(profile.telegramUserId()), Optional.of(invitation.tokenPrefix()), invalidReason.get(), "Invitation cannot be confirmed");
            return ConfirmTelegramLinkResponse.invalid(invalidReason.get());
        }

        Optional<Client> client = clientRepository.findById(invitation.clientId())
                .filter(found -> found.companyId().equals(invitation.companyId()))
                .filter(Client::active);
        if (client.isEmpty()) {
            auditFailure(Optional.of(invitation.companyId()), Optional.of(invitation.clientId()), Optional.of(invitation.id()), Optional.of(profile.telegramUserId()), Optional.of(invitation.tokenPrefix()), "CLIENT_NOT_AVAILABLE", "Client is not available");
            return ConfirmTelegramLinkResponse.invalid("CLIENT_NOT_AVAILABLE");
        }
        if (accountRepository.existsActiveByCompanyIdAndClientId(invitation.companyId(), invitation.clientId())) {
            auditFailure(Optional.of(invitation.companyId()), Optional.of(invitation.clientId()), Optional.of(invitation.id()), Optional.of(profile.telegramUserId()), Optional.of(invitation.tokenPrefix()), "CLIENT_ALREADY_LINKED", "Client already has an active Telegram link");
            return ConfirmTelegramLinkResponse.invalid("CLIENT_ALREADY_LINKED");
        }
        if (accountRepository.existsActiveByCompanyIdAndTelegramUserId(invitation.companyId(), profile.telegramUserId())) {
            auditPort.record(event(Optional.of(invitation.companyId()), Optional.of(invitation.clientId()), Optional.of(invitation.id()), Optional.of(profile.telegramUserId()),
                    TelegramLinkEventType.DUPLICATE_TELEGRAM_ACCOUNT_DETECTED, TelegramLinkEventResult.FAILURE,
                    "TELEGRAM_ACCOUNT_ALREADY_LINKED", "Telegram account is already linked", tokenMetadata(invitation.tokenPrefix())));
            return ConfirmTelegramLinkResponse.invalid("TELEGRAM_ACCOUNT_ALREADY_LINKED");
        }

        ClientTelegramAccount account = new ClientTelegramAccount(
                accountIdGenerator.nextId(),
                invitation.companyId(),
                invitation.clientId(),
                profile.telegramUserId(),
                profile.telegramChatId(),
                profile.telegramUsername(),
                profile.telegramFirstName(),
                profile.telegramLastName(),
                Optional.of(invitation.id()),
                TelegramAccountStatus.ACTIVE,
                now,
                Optional.empty(),
                now,
                now);
        ClientTelegramAccount savedAccount = accountRepository.save(account);
        TelegramInvitation usedInvitation = invitationRepository.save(invitation.markUsed(profile, now));
        auditPort.record(event(Optional.of(usedInvitation.companyId()), Optional.of(usedInvitation.clientId()), Optional.of(usedInvitation.id()), Optional.of(profile.telegramUserId()),
                TelegramLinkEventType.CLIENT_TELEGRAM_LINKED, TelegramLinkEventResult.SUCCESS,
                "CLIENT_TELEGRAM_LINKED", "Client Telegram account linked", tokenMetadata(usedInvitation.tokenPrefix())));
        return ConfirmTelegramLinkResponse.confirmed(savedAccount.clientId(), savedAccount.id(), savedAccount.telegramUserId(), savedAccount.linkedAt());
    }

    private Optional<String> invalidInvitationReason(TelegramInvitation invitation, Instant now) {
        if (invitation.status() == TelegramInvitationStatus.USED) return Optional.of("INVITATION_ALREADY_USED");
        if (invitation.status() == TelegramInvitationStatus.REVOKED) return Optional.of("INVITATION_REVOKED");
        if (!invitation.expiresAt().isAfter(now)) return Optional.of("INVITATION_EXPIRED");
        if (invitation.status() != TelegramInvitationStatus.PENDING) return Optional.of("INVALID_TOKEN");
        return Optional.empty();
    }

    private void auditFailure(
            Optional<CompanyId> companyId,
            Optional<ClientId> clientId,
            Optional<InvitationTokenId> invitationId,
            Optional<Long> telegramUserId,
            Optional<String> tokenPrefix,
            String reasonCode,
            String message) {
        auditPort.record(event(companyId, clientId, invitationId, telegramUserId, TelegramLinkEventType.CLIENT_TELEGRAM_LINKED,
                TelegramLinkEventResult.FAILURE, reasonCode, message, tokenPrefix.map(this::tokenMetadata).orElse("{}")));
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
