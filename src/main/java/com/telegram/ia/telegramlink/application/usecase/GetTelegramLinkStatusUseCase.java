package com.telegram.ia.telegramlink.application.usecase;

import com.telegram.ia.telegramlink.application.error.TelegramLinkingApplicationException;
import com.telegram.ia.telegramlink.application.port.in.GetTelegramLinkStatusPort;
import com.telegram.ia.telegramlink.application.port.out.ClientAssignmentRepositoryPort;
import com.telegram.ia.telegramlink.application.port.out.ClientRepositoryPort;
import com.telegram.ia.telegramlink.application.port.out.ClientTelegramAccountRepositoryPort;
import com.telegram.ia.telegramlink.application.port.out.ClockPort;
import com.telegram.ia.telegramlink.application.port.out.CurrentUserProviderPort;
import com.telegram.ia.telegramlink.application.port.out.TelegramInvitationRepositoryPort;
import com.telegram.ia.telegramlink.application.query.GetTelegramLinkStatusQuery;
import com.telegram.ia.telegramlink.application.response.AuthenticatedUser;
import com.telegram.ia.telegramlink.application.response.TelegramLinkStatus;
import com.telegram.ia.telegramlink.application.response.TelegramLinkStatusResponse;
import com.telegram.ia.telegramlink.domain.model.Client;
import com.telegram.ia.telegramlink.domain.model.ClientTelegramAccount;
import com.telegram.ia.telegramlink.domain.model.TelegramInvitation;
import com.telegram.ia.telegramlink.domain.policy.InvitationAuthorizationPolicy;
import com.telegram.ia.telegramlink.domain.policy.InvitationAuthorizationResult;
import java.util.Optional;

public class GetTelegramLinkStatusUseCase implements GetTelegramLinkStatusPort {
    private final CurrentUserProviderPort currentUserProvider;
    private final ClientRepositoryPort clientRepository;
    private final ClientAssignmentRepositoryPort assignmentRepository;
    private final TelegramInvitationRepositoryPort invitationRepository;
    private final ClientTelegramAccountRepositoryPort accountRepository;
    private final ClockPort clock;

    public GetTelegramLinkStatusUseCase(
            CurrentUserProviderPort currentUserProvider,
            ClientRepositoryPort clientRepository,
            ClientAssignmentRepositoryPort assignmentRepository,
            TelegramInvitationRepositoryPort invitationRepository,
            ClientTelegramAccountRepositoryPort accountRepository,
            ClockPort clock) {
        this.currentUserProvider = currentUserProvider;
        this.clientRepository = clientRepository;
        this.assignmentRepository = assignmentRepository;
        this.invitationRepository = invitationRepository;
        this.accountRepository = accountRepository;
        this.clock = clock;
    }

    @Override
    public TelegramLinkStatusResponse execute(GetTelegramLinkStatusQuery query) {
        AuthenticatedUser user = currentUserProvider.currentUser();
        Client client = clientRepository.findById(query.clientId())
                .orElseThrow(() -> new TelegramLinkingApplicationException("CLIENT_NOT_FOUND", "Client was not found"));
        if (!client.companyId().equals(user.companyId())) {
            throw new TelegramLinkingApplicationException("CLIENT_NOT_FOUND", "Client was not found");
        }
        boolean assigned = assignmentRepository.existsActiveAssignment(user.companyUserId(), client.id(), user.companyId());
        InvitationAuthorizationResult authorization = InvitationAuthorizationPolicy.canInvite(user.toCompanyUser(), client, assigned);
        if (!authorization.allowed()) {
            String reason = authorization.denialReason().orElseThrow().name();
            throw new TelegramLinkingApplicationException(reason, "User is not allowed to query Telegram link status");
        }

        Optional<ClientTelegramAccount> activeAccount = accountRepository.findActiveByCompanyIdAndClientId(user.companyId(), client.id());
        if (activeAccount.isPresent()) {
            ClientTelegramAccount account = activeAccount.get();
            return new TelegramLinkStatusResponse(client.id(), TelegramLinkStatus.LINKED, Optional.empty(), Optional.empty(),
                    Optional.of(account.telegramUserId()), Optional.ofNullable(account.telegramUsername()), Optional.of(account.linkedAt()));
        }

        Optional<TelegramInvitation> pending = invitationRepository.findPendingByClientId(user.companyId(), client.id(), clock.now());
        if (pending.isPresent()) {
            TelegramInvitation invitation = pending.get();
            return new TelegramLinkStatusResponse(client.id(), TelegramLinkStatus.INVITATION_PENDING,
                    Optional.of(invitation.id()), Optional.of(invitation.expiresAt()), Optional.empty(), Optional.empty(), Optional.empty());
        }
        return TelegramLinkStatusResponse.notLinked(client.id());
    }
}
