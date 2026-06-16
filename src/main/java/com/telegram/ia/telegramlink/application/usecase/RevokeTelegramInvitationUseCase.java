package com.telegram.ia.telegramlink.application.usecase;

import com.telegram.ia.telegramlink.application.command.RevokeTelegramInvitationCommand;
import com.telegram.ia.telegramlink.application.error.TelegramLinkingApplicationException;
import com.telegram.ia.telegramlink.application.port.in.RevokeTelegramInvitationPort;
import com.telegram.ia.telegramlink.application.port.out.ClientAssignmentRepositoryPort;
import com.telegram.ia.telegramlink.application.port.out.ClientRepositoryPort;
import com.telegram.ia.telegramlink.application.port.out.ClockPort;
import com.telegram.ia.telegramlink.application.port.out.CurrentUserProviderPort;
import com.telegram.ia.telegramlink.application.port.out.TelegramInvitationAuditPort;
import com.telegram.ia.telegramlink.application.port.out.TelegramInvitationRepositoryPort;
import com.telegram.ia.telegramlink.application.port.out.TelegramLinkEventIdGeneratorPort;
import com.telegram.ia.telegramlink.application.port.out.TransactionRunnerPort;
import com.telegram.ia.telegramlink.application.response.AuthenticatedUser;
import com.telegram.ia.telegramlink.application.response.RevokeTelegramInvitationResponse;
import com.telegram.ia.telegramlink.domain.model.Client;
import com.telegram.ia.telegramlink.domain.model.TelegramInvitation;
import com.telegram.ia.telegramlink.domain.model.TelegramLinkEvent;
import com.telegram.ia.telegramlink.domain.model.TelegramLinkEventResult;
import com.telegram.ia.telegramlink.domain.model.TelegramLinkEventType;
import com.telegram.ia.telegramlink.domain.policy.InvitationAuthorizationPolicy;
import com.telegram.ia.telegramlink.domain.policy.InvitationAuthorizationResult;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;

public class RevokeTelegramInvitationUseCase implements RevokeTelegramInvitationPort {
    private final CurrentUserProviderPort currentUserProvider;
    private final ClientRepositoryPort clientRepository;
    private final ClientAssignmentRepositoryPort assignmentRepository;
    private final TelegramInvitationRepositoryPort invitationRepository;
    private final TelegramLinkEventIdGeneratorPort eventIdGenerator;
    private final TelegramInvitationAuditPort auditPort;
    private final ClockPort clock;
    private final TransactionRunnerPort transactionRunner;

    public RevokeTelegramInvitationUseCase(
            CurrentUserProviderPort currentUserProvider,
            ClientRepositoryPort clientRepository,
            ClientAssignmentRepositoryPort assignmentRepository,
            TelegramInvitationRepositoryPort invitationRepository,
            TelegramLinkEventIdGeneratorPort eventIdGenerator,
            TelegramInvitationAuditPort auditPort,
            ClockPort clock,
            TransactionRunnerPort transactionRunner) {
        this.currentUserProvider = currentUserProvider;
        this.clientRepository = clientRepository;
        this.assignmentRepository = assignmentRepository;
        this.invitationRepository = invitationRepository;
        this.eventIdGenerator = eventIdGenerator;
        this.auditPort = auditPort;
        this.clock = clock;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public RevokeTelegramInvitationResponse execute(RevokeTelegramInvitationCommand command) {
        return transactionRunner.inWriteTransaction(revoke(command));
    }

    private Supplier<RevokeTelegramInvitationResponse> revoke(RevokeTelegramInvitationCommand command) {
        return () -> {
            AuthenticatedUser user = currentUserProvider.currentUser();
            Instant now = clock.now();
            TelegramInvitation invitation = invitationRepository
                    .findPendingByIdAndCompanyId(command.invitationId(), user.companyId(), now)
                    .orElseThrow(() -> new TelegramLinkingApplicationException("INVITATION_NOT_FOUND", "Pending invitation was not found"));
            Client client = clientRepository.findById(invitation.clientId())
                    .orElseThrow(() -> new TelegramLinkingApplicationException("CLIENT_NOT_FOUND", "Client was not found"));
            boolean assigned = assignmentRepository.existsActiveAssignment(user.companyUserId(), client.id(), user.companyId());
            InvitationAuthorizationResult authorization = InvitationAuthorizationPolicy.canInvite(user.toCompanyUser(), client, assigned);
            if (!authorization.allowed()) {
                String reason = authorization.denialReason().orElseThrow().name();
                throw new TelegramLinkingApplicationException(reason, "User is not allowed to revoke invitation");
            }
            TelegramInvitation revoked = invitationRepository.save(invitation.revoke(user.companyUserId(), now));
            auditPort.record(new TelegramLinkEvent(eventIdGenerator.nextId(), Optional.of(user.companyId()), Optional.of(revoked.clientId()),
                    Optional.of(revoked.id()), Optional.of(user.companyUserId()), Optional.empty(), Optional.empty(),
                    TelegramLinkEventType.INVITATION_REVOKED, TelegramLinkEventResult.SUCCESS, "INVITATION_REVOKED",
                    "Invitation revoked", "{}", now));
            return new RevokeTelegramInvitationResponse(revoked.id(), revoked.status(), now);
        };
    }
}
