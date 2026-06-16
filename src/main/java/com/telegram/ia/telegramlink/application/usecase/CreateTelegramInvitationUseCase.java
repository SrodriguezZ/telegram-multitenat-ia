package com.telegram.ia.telegramlink.application.usecase;

import com.telegram.ia.telegramlink.application.command.CreateTelegramInvitationCommand;
import com.telegram.ia.telegramlink.application.error.TelegramLinkingApplicationException;
import com.telegram.ia.telegramlink.application.port.in.CreateTelegramInvitationPort;
import com.telegram.ia.telegramlink.application.port.out.ClientAssignmentRepositoryPort;
import com.telegram.ia.telegramlink.application.port.out.ClientRepositoryPort;
import com.telegram.ia.telegramlink.application.port.out.ClientTelegramAccountRepositoryPort;
import com.telegram.ia.telegramlink.application.port.out.ClockPort;
import com.telegram.ia.telegramlink.application.port.out.CurrentUserProviderPort;
import com.telegram.ia.telegramlink.application.port.out.TelegramInvitationAuditPort;
import com.telegram.ia.telegramlink.application.port.out.TelegramInvitationIdGeneratorPort;
import com.telegram.ia.telegramlink.application.port.out.TelegramInvitationLinkBuilderPort;
import com.telegram.ia.telegramlink.application.port.out.TelegramInvitationRepositoryPort;
import com.telegram.ia.telegramlink.application.port.out.TelegramLinkEventIdGeneratorPort;
import com.telegram.ia.telegramlink.application.port.out.TokenGeneratorPort;
import com.telegram.ia.telegramlink.application.port.out.TokenHashingPort;
import com.telegram.ia.telegramlink.application.port.out.TransactionRunnerPort;
import com.telegram.ia.telegramlink.application.response.AuthenticatedUser;
import com.telegram.ia.telegramlink.application.response.CreateTelegramInvitationResponse;
import com.telegram.ia.telegramlink.domain.model.Client;
import com.telegram.ia.telegramlink.domain.model.TelegramInvitation;
import com.telegram.ia.telegramlink.domain.model.TelegramLinkEvent;
import com.telegram.ia.telegramlink.domain.model.TelegramLinkEventResult;
import com.telegram.ia.telegramlink.domain.model.TelegramLinkEventType;
import com.telegram.ia.telegramlink.domain.policy.InvitationAuthorizationPolicy;
import com.telegram.ia.telegramlink.domain.policy.InvitationAuthorizationResult;
import com.telegram.ia.telegramlink.domain.valueobject.ClientId;
import com.telegram.ia.telegramlink.domain.valueobject.CompanyId;
import com.telegram.ia.telegramlink.domain.valueobject.InvitationTokenId;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public class CreateTelegramInvitationUseCase implements CreateTelegramInvitationPort {
    private static final Duration INVITATION_TTL = Duration.ofHours(48);

    private final CurrentUserProviderPort currentUserProvider;
    private final ClientRepositoryPort clientRepository;
    private final ClientAssignmentRepositoryPort assignmentRepository;
    private final TelegramInvitationRepositoryPort invitationRepository;
    private final ClientTelegramAccountRepositoryPort accountRepository;
    private final TokenGeneratorPort tokenGenerator;
    private final TokenHashingPort tokenHashing;
    private final TelegramInvitationIdGeneratorPort invitationIdGenerator;
    private final TelegramLinkEventIdGeneratorPort eventIdGenerator;
    private final TelegramInvitationLinkBuilderPort linkBuilder;
    private final TelegramInvitationAuditPort auditPort;
    private final ClockPort clock;
    private final TransactionRunnerPort transactionRunner;

    public CreateTelegramInvitationUseCase(
            CurrentUserProviderPort currentUserProvider,
            ClientRepositoryPort clientRepository,
            ClientAssignmentRepositoryPort assignmentRepository,
            TelegramInvitationRepositoryPort invitationRepository,
            ClientTelegramAccountRepositoryPort accountRepository,
            TokenGeneratorPort tokenGenerator,
            TokenHashingPort tokenHashing,
            TelegramInvitationIdGeneratorPort invitationIdGenerator,
            TelegramLinkEventIdGeneratorPort eventIdGenerator,
            TelegramInvitationLinkBuilderPort linkBuilder,
            TelegramInvitationAuditPort auditPort,
            ClockPort clock,
            TransactionRunnerPort transactionRunner) {
        this.currentUserProvider = currentUserProvider;
        this.clientRepository = clientRepository;
        this.assignmentRepository = assignmentRepository;
        this.invitationRepository = invitationRepository;
        this.accountRepository = accountRepository;
        this.tokenGenerator = tokenGenerator;
        this.tokenHashing = tokenHashing;
        this.invitationIdGenerator = invitationIdGenerator;
        this.eventIdGenerator = eventIdGenerator;
        this.linkBuilder = linkBuilder;
        this.auditPort = auditPort;
        this.clock = clock;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public CreateTelegramInvitationResponse execute(CreateTelegramInvitationCommand command) {
        return transactionRunner.inWriteTransaction(() -> create(command));
    }

    private CreateTelegramInvitationResponse create(CreateTelegramInvitationCommand command) {
        AuthenticatedUser user = currentUserProvider.currentUser();
        Instant now = clock.now();
        Client client = clientRepository.findById(command.clientId())
                .orElseThrow(() -> fail(user.companyId(), command.clientId(), "CLIENT_NOT_FOUND", "Client was not found"));
        if (!client.companyId().equals(user.companyId())) {
            throw fail(user.companyId(), command.clientId(), "CLIENT_NOT_FOUND", "Client was not found");
        }

        boolean assigned = assignmentRepository.existsActiveAssignment(user.companyUserId(), client.id(), user.companyId());
        InvitationAuthorizationResult authorization = InvitationAuthorizationPolicy.canInvite(user.toCompanyUser(), client, assigned);
        if (!authorization.allowed()) {
            String reason = authorization.denialReason().orElseThrow().name();
            throw fail(user.companyId(), client.id(), reason, "User is not allowed to create invitation");
        }
        if (accountRepository.existsActiveByCompanyIdAndClientId(user.companyId(), client.id())) {
            throw fail(user.companyId(), client.id(), "CLIENT_ALREADY_LINKED", "Client already has an active Telegram link");
        }
        if (invitationRepository.findPendingByClientId(user.companyId(), client.id(), now).isPresent()) {
            throw fail(user.companyId(), client.id(), "INVITATION_ALREADY_PENDING", "Client already has a pending invitation");
        }

        String rawToken = tokenGenerator.generateToken();
        String tokenHash = tokenHashing.hash(rawToken);
        String tokenPrefix = rawToken.substring(0, Math.min(4, rawToken.length()));
        Instant expiresAt = now.plus(INVITATION_TTL);
        TelegramInvitation invitation = TelegramInvitation.createPending(
                invitationIdGenerator.nextId(), user.companyId(), client.id(), user.companyUserId(), tokenHash, tokenPrefix, now, expiresAt);
        TelegramInvitation saved = invitationRepository.save(invitation);
        auditSuccess(user, saved.id(), client.id(), tokenPrefix);
        return new CreateTelegramInvitationResponse(saved.id(), saved.status(), linkBuilder.buildLink(rawToken), saved.expiresAt(), saved.tokenPrefix());
    }

    private TelegramLinkingApplicationException fail(CompanyId companyId, ClientId clientId, String reasonCode, String message) {
        auditPort.record(event(companyId, clientId, Optional.empty(), TelegramLinkEventType.INVITATION_CREATED,
                TelegramLinkEventResult.FAILURE, reasonCode, message, "{}"));
        return new TelegramLinkingApplicationException(reasonCode, message);
    }

    private void auditSuccess(AuthenticatedUser user, InvitationTokenId invitationId, ClientId clientId, String tokenPrefix) {
        auditPort.record(event(user.companyId(), clientId, Optional.of(invitationId), TelegramLinkEventType.INVITATION_CREATED,
                TelegramLinkEventResult.SUCCESS, "INVITATION_CREATED", "Invitation created", "{\"tokenPrefix\":\"" + tokenPrefix + "\"}"));
    }

    private TelegramLinkEvent event(
            CompanyId companyId,
            ClientId clientId,
            Optional<InvitationTokenId> invitationId,
            TelegramLinkEventType type,
            TelegramLinkEventResult result,
            String reasonCode,
            String message,
            String metadata) {
        AuthenticatedUser user = currentUserProvider.currentUser();
        return new TelegramLinkEvent(eventIdGenerator.nextId(), Optional.of(companyId), Optional.of(clientId), invitationId,
                Optional.of(user.companyUserId()), Optional.empty(), Optional.empty(), type, result, reasonCode, message, metadata, clock.now());
    }
}
