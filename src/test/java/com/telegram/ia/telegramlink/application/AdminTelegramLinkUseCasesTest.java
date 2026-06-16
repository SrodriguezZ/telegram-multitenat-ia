package com.telegram.ia.telegramlink.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.telegram.ia.telegramlink.application.command.CreateTelegramInvitationCommand;
import com.telegram.ia.telegramlink.application.command.RevokeTelegramInvitationCommand;
import com.telegram.ia.telegramlink.application.error.TelegramLinkingApplicationException;
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
import com.telegram.ia.telegramlink.application.query.GetTelegramLinkStatusQuery;
import com.telegram.ia.telegramlink.application.response.AuthenticatedUser;
import com.telegram.ia.telegramlink.application.response.CreateTelegramInvitationResponse;
import com.telegram.ia.telegramlink.application.response.TelegramLinkStatus;
import com.telegram.ia.telegramlink.application.response.TelegramLinkStatusResponse;
import com.telegram.ia.telegramlink.application.usecase.CreateTelegramInvitationUseCase;
import com.telegram.ia.telegramlink.application.usecase.GetTelegramLinkStatusUseCase;
import com.telegram.ia.telegramlink.application.usecase.RevokeTelegramInvitationUseCase;
import com.telegram.ia.telegramlink.domain.model.Client;
import com.telegram.ia.telegramlink.domain.model.ClientStatus;
import com.telegram.ia.telegramlink.domain.model.ClientTelegramAccount;
import com.telegram.ia.telegramlink.domain.model.CompanyUserRole;
import com.telegram.ia.telegramlink.domain.model.CompanyUserStatus;
import com.telegram.ia.telegramlink.domain.model.TelegramAccountStatus;
import com.telegram.ia.telegramlink.domain.model.TelegramInvitation;
import com.telegram.ia.telegramlink.domain.model.TelegramInvitationStatus;
import com.telegram.ia.telegramlink.domain.model.TelegramLinkEvent;
import com.telegram.ia.telegramlink.domain.model.TelegramLinkEventResult;
import com.telegram.ia.telegramlink.domain.model.TelegramLinkEventType;
import com.telegram.ia.telegramlink.domain.valueobject.ClientId;
import com.telegram.ia.telegramlink.domain.valueobject.CompanyId;
import com.telegram.ia.telegramlink.domain.valueobject.CompanyUserId;
import com.telegram.ia.telegramlink.domain.valueobject.InvitationTokenId;
import com.telegram.ia.telegramlink.domain.valueobject.TelegramAccountId;
import com.telegram.ia.telegramlink.domain.valueobject.TelegramLinkEventId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class AdminTelegramLinkUseCasesTest {

    private static final Instant NOW = Instant.parse("2026-06-14T12:00:00Z");
    private static final CompanyId COMPANY_ID = new CompanyId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final CompanyUserId ADMIN_ID = new CompanyUserId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
    private static final CompanyUserId AGENT_ID = new CompanyUserId(UUID.fromString("00000000-0000-0000-0000-000000000003"));
    private static final ClientId CLIENT_ID = new ClientId(UUID.fromString("00000000-0000-0000-0000-000000000004"));
    private static final InvitationTokenId INVITATION_ID = new InvitationTokenId(UUID.fromString("00000000-0000-0000-0000-000000000005"));
    private static final TelegramLinkEventId EVENT_ID = new TelegramLinkEventId(UUID.fromString("00000000-0000-0000-0000-000000000006"));

    @Test
    void adminCreatesInvitationForActiveUnlinkedCompanyClientWithoutStoringRawTokenInAudit() {
        Scenario scenario = Scenario.withUser(admin());
        scenario.clients.put(CLIENT_ID, activeClient(CLIENT_ID));

        CreateTelegramInvitationResponse response = scenario.createUseCase().execute(new CreateTelegramInvitationCommand(CLIENT_ID));

        TelegramInvitation stored = scenario.invitations.get(INVITATION_ID);
        assertThat(response.invitationId()).isEqualTo(INVITATION_ID);
        assertThat(response.status()).isEqualTo(TelegramInvitationStatus.PENDING);
        assertThat(response.expiresAt()).isEqualTo(NOW.plusSeconds(172_800));
        assertThat(response.tokenPrefix()).isEqualTo("raw-");
        assertThat(response.link()).isEqualTo("https://t.me/test_bot?start=raw-token-123");
        assertThat(stored.tokenHash()).isEqualTo("hashed-raw-token-123");
        assertThat(stored.tokenPrefix()).isEqualTo("raw-");
        assertThat(scenario.auditEvents)
                .extracting(TelegramLinkEvent::eventType)
                .containsExactly(TelegramLinkEventType.INVITATION_CREATED);
        assertThat(scenario.auditEvents.get(0).metadata()).doesNotContain("raw-token-123");
    }

    @Test
    void duplicatePendingInvitationFailsWithConflictAndDoesNotCreateNewInvitation() {
        Scenario scenario = Scenario.withUser(admin());
        scenario.clients.put(CLIENT_ID, activeClient(CLIENT_ID));
        scenario.invitations.put(INVITATION_ID, pendingInvitation(CLIENT_ID));

        assertThatThrownBy(() -> scenario.createUseCase().execute(new CreateTelegramInvitationCommand(CLIENT_ID)))
                .isInstanceOf(TelegramLinkingApplicationException.class)
                .extracting("errorCode")
                .isEqualTo("INVITATION_ALREADY_PENDING");

        assertThat(scenario.invitations).hasSize(1);
        assertThat(scenario.auditEvents)
                .extracting(TelegramLinkEvent::eventType, TelegramLinkEvent::result, TelegramLinkEvent::reasonCode)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(
                        TelegramLinkEventType.INVITATION_CREATED,
                        TelegramLinkEventResult.FAILURE,
                        "INVITATION_ALREADY_PENDING"));
    }

    @Test
    void unassignedAgentCannotCreateInvitationForClient() {
        Scenario scenario = Scenario.withUser(agent());
        scenario.clients.put(CLIENT_ID, activeClient(CLIENT_ID));

        assertThatThrownBy(() -> scenario.createUseCase().execute(new CreateTelegramInvitationCommand(CLIENT_ID)))
                .isInstanceOf(TelegramLinkingApplicationException.class)
                .extracting("errorCode")
                .isEqualTo("CLIENT_NOT_ASSIGNED_TO_AGENT");

        assertThat(scenario.invitations).isEmpty();
        assertThat(scenario.auditEvents)
                .extracting(TelegramLinkEvent::eventType, TelegramLinkEvent::result, TelegramLinkEvent::reasonCode)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(
                        TelegramLinkEventType.INVITATION_CREATED,
                        TelegramLinkEventResult.FAILURE,
                        "CLIENT_NOT_ASSIGNED_TO_AGENT"));
    }

    @Test
    void assignedAgentCanCreateInvitationForAssignedActiveClient() {
        Scenario scenario = Scenario.withUser(agent());
        scenario.clients.put(CLIENT_ID, activeClient(CLIENT_ID));
        scenario.assignedClientIds.add(CLIENT_ID);

        CreateTelegramInvitationResponse response = scenario.createUseCase().execute(new CreateTelegramInvitationCommand(CLIENT_ID));

        assertThat(response.status()).isEqualTo(TelegramInvitationStatus.PENDING);
        assertThat(scenario.invitations).containsKey(INVITATION_ID);
    }

    @Test
    void revokeMarksCompanyPendingInvitationAsRevokedAndAuditsActor() {
        Scenario scenario = Scenario.withUser(admin());
        scenario.clients.put(CLIENT_ID, activeClient(CLIENT_ID));
        scenario.invitations.put(INVITATION_ID, pendingInvitation(CLIENT_ID));

        var response = scenario.revokeUseCase().execute(new RevokeTelegramInvitationCommand(INVITATION_ID));

        TelegramInvitation revoked = scenario.invitations.get(INVITATION_ID);
        assertThat(response.invitationId()).isEqualTo(INVITATION_ID);
        assertThat(response.status()).isEqualTo(TelegramInvitationStatus.REVOKED);
        assertThat(revoked.status()).isEqualTo(TelegramInvitationStatus.REVOKED);
        assertThat(revoked.revokedByUserId()).contains(ADMIN_ID);
        assertThat(scenario.auditEvents)
                .extracting(TelegramLinkEvent::eventType, TelegramLinkEvent::result)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(
                        TelegramLinkEventType.INVITATION_REVOKED,
                        TelegramLinkEventResult.SUCCESS));
    }

    @Test
    void suspendedUserCannotRevokePendingInvitation() {
        Scenario scenario = Scenario.withUser(suspendedAdmin());
        scenario.clients.put(CLIENT_ID, activeClient(CLIENT_ID));
        scenario.invitations.put(INVITATION_ID, pendingInvitation(CLIENT_ID));

        assertThatThrownBy(() -> scenario.revokeUseCase().execute(new RevokeTelegramInvitationCommand(INVITATION_ID)))
                .isInstanceOf(TelegramLinkingApplicationException.class)
                .extracting("errorCode")
                .isEqualTo("USER_NOT_ACTIVE");

        assertThat(scenario.invitations.get(INVITATION_ID).status()).isEqualTo(TelegramInvitationStatus.PENDING);
    }

    @Test
    void unassignedAgentCannotRevokePendingInvitationForClient() {
        Scenario scenario = Scenario.withUser(agent());
        scenario.clients.put(CLIENT_ID, activeClient(CLIENT_ID));
        scenario.invitations.put(INVITATION_ID, pendingInvitation(CLIENT_ID));

        assertThatThrownBy(() -> scenario.revokeUseCase().execute(new RevokeTelegramInvitationCommand(INVITATION_ID)))
                .isInstanceOf(TelegramLinkingApplicationException.class)
                .extracting("errorCode")
                .isEqualTo("CLIENT_NOT_ASSIGNED_TO_AGENT");

        assertThat(scenario.invitations.get(INVITATION_ID).status()).isEqualTo(TelegramInvitationStatus.PENDING);
    }

    @Test
    void statusReflectsNotLinkedPendingAndLinkedStatesWithinCompanyScope() {
        Scenario scenario = Scenario.withUser(admin());
        ClientId notLinkedClientId = CLIENT_ID;
        ClientId pendingClientId = new ClientId(UUID.fromString("00000000-0000-0000-0000-000000000007"));
        ClientId linkedClientId = new ClientId(UUID.fromString("00000000-0000-0000-0000-000000000008"));
        scenario.clients.put(notLinkedClientId, activeClient(notLinkedClientId));
        scenario.clients.put(pendingClientId, activeClient(pendingClientId));
        scenario.clients.put(linkedClientId, activeClient(linkedClientId));
        scenario.invitations.put(INVITATION_ID, pendingInvitation(pendingClientId));
        scenario.accounts.put(linkedClientId, activeAccount(linkedClientId));

        TelegramLinkStatusResponse notLinked = scenario.statusUseCase().execute(new GetTelegramLinkStatusQuery(notLinkedClientId));
        TelegramLinkStatusResponse pending = scenario.statusUseCase().execute(new GetTelegramLinkStatusQuery(pendingClientId));
        TelegramLinkStatusResponse linked = scenario.statusUseCase().execute(new GetTelegramLinkStatusQuery(linkedClientId));

        assertThat(notLinked.status()).isEqualTo(TelegramLinkStatus.NOT_LINKED);
        assertThat(notLinked.invitationId()).isEmpty();
        assertThat(pending.status()).isEqualTo(TelegramLinkStatus.INVITATION_PENDING);
        assertThat(pending.invitationId()).contains(INVITATION_ID);
        assertThat(linked.status()).isEqualTo(TelegramLinkStatus.LINKED);
        assertThat(linked.telegramUserId()).contains(42L);
    }

    @Test
    void suspendedUserCannotQueryTelegramLinkStatus() {
        Scenario scenario = Scenario.withUser(suspendedAdmin());
        scenario.clients.put(CLIENT_ID, activeClient(CLIENT_ID));

        assertThatThrownBy(() -> scenario.statusUseCase().execute(new GetTelegramLinkStatusQuery(CLIENT_ID)))
                .isInstanceOf(TelegramLinkingApplicationException.class)
                .extracting("errorCode")
                .isEqualTo("USER_NOT_ACTIVE");
    }

    @Test
    void unassignedAgentCannotQueryTelegramLinkStatusForClient() {
        Scenario scenario = Scenario.withUser(agent());
        scenario.clients.put(CLIENT_ID, activeClient(CLIENT_ID));

        assertThatThrownBy(() -> scenario.statusUseCase().execute(new GetTelegramLinkStatusQuery(CLIENT_ID)))
                .isInstanceOf(TelegramLinkingApplicationException.class)
                .extracting("errorCode")
                .isEqualTo("CLIENT_NOT_ASSIGNED_TO_AGENT");
    }

    @Test
    void crossCompanyStatusQueryIsRejected() {
        Scenario scenario = Scenario.withUser(admin());
        scenario.clients.put(CLIENT_ID, clientInOtherCompany(CLIENT_ID));

        assertThatThrownBy(() -> scenario.statusUseCase().execute(new GetTelegramLinkStatusQuery(CLIENT_ID)))
                .isInstanceOf(TelegramLinkingApplicationException.class)
                .extracting("errorCode")
                .isEqualTo("CLIENT_NOT_FOUND");
    }

    private static AuthenticatedUser admin() {
        return new AuthenticatedUser(ADMIN_ID, COMPANY_ID, CompanyUserRole.ADMIN, CompanyUserStatus.ACTIVE);
    }

    private static AuthenticatedUser suspendedAdmin() {
        return new AuthenticatedUser(ADMIN_ID, COMPANY_ID, CompanyUserRole.ADMIN, CompanyUserStatus.SUSPENDED);
    }

    private static AuthenticatedUser agent() {
        return new AuthenticatedUser(AGENT_ID, COMPANY_ID, CompanyUserRole.AGENT, CompanyUserStatus.ACTIVE);
    }

    private static Client activeClient(ClientId clientId) {
        return new Client(clientId, COMPANY_ID, "Jane Doe", ClientStatus.ACTIVE);
    }

    private static Client clientInOtherCompany(ClientId clientId) {
        return new Client(clientId, new CompanyId(UUID.fromString("00000000-0000-0000-0000-000000000010")), "Jane Doe", ClientStatus.ACTIVE);
    }

    private static TelegramInvitation pendingInvitation(ClientId clientId) {
        return TelegramInvitation.createPending(
                INVITATION_ID,
                COMPANY_ID,
                clientId,
                ADMIN_ID,
                "existing-hash",
                "exis",
                NOW.minusSeconds(60),
                NOW.plusSeconds(172_800));
    }

    private static ClientTelegramAccount activeAccount(ClientId clientId) {
        return new ClientTelegramAccount(
                new TelegramAccountId(UUID.fromString("00000000-0000-0000-0000-000000000009")),
                COMPANY_ID,
                clientId,
                42L,
                84L,
                "telegram_user",
                "Telegram",
                "User",
                Optional.empty(),
                TelegramAccountStatus.ACTIVE,
                NOW.minusSeconds(120),
                Optional.empty(),
                NOW.minusSeconds(120),
                NOW.minusSeconds(120));
    }

    private static final class Scenario {
        private final AuthenticatedUser user;
        private final Map<ClientId, Client> clients = new HashMap<>();
        private final Map<InvitationTokenId, TelegramInvitation> invitations = new HashMap<>();
        private final Map<ClientId, ClientTelegramAccount> accounts = new HashMap<>();
        private final List<ClientId> assignedClientIds = new ArrayList<>();
        private final List<TelegramLinkEvent> auditEvents = new ArrayList<>();

        private Scenario(AuthenticatedUser user) {
            this.user = user;
        }

        static Scenario withUser(AuthenticatedUser user) {
            return new Scenario(user);
        }

        CreateTelegramInvitationUseCase createUseCase() {
            return new CreateTelegramInvitationUseCase(
                    currentUserProvider(),
                    clientRepository(),
                    assignmentRepository(),
                    invitationRepository(),
                    accountRepository(),
                    tokenGenerator(),
                    tokenHashing(),
                    invitationIdGenerator(),
                    eventIdGenerator(),
                    linkBuilder(),
                    auditPort(),
                    clock(),
                    transactionRunner());
        }

        RevokeTelegramInvitationUseCase revokeUseCase() {
            return new RevokeTelegramInvitationUseCase(
                    currentUserProvider(),
                    clientRepository(),
                    assignmentRepository(),
                    invitationRepository(),
                    eventIdGenerator(),
                    auditPort(),
                    clock(),
                    transactionRunner());
        }

        GetTelegramLinkStatusUseCase statusUseCase() {
            return new GetTelegramLinkStatusUseCase(currentUserProvider(), clientRepository(), assignmentRepository(), invitationRepository(), accountRepository(), clock());
        }

        private CurrentUserProviderPort currentUserProvider() {
            return () -> user;
        }

        private ClientRepositoryPort clientRepository() {
            return clientId -> Optional.ofNullable(clients.get(clientId));
        }

        private ClientAssignmentRepositoryPort assignmentRepository() {
            return (companyUserId, clientId, companyId) -> assignedClientIds.contains(clientId);
        }

        private TelegramInvitationRepositoryPort invitationRepository() {
            return new TelegramInvitationRepositoryPort() {
                @Override
                public TelegramInvitation save(TelegramInvitation invitation) {
                    invitations.put(invitation.id(), invitation);
                    return invitation;
                }

                @Override
                public Optional<TelegramInvitation> findPendingByClientId(CompanyId companyId, ClientId clientId, Instant now) {
                    return invitations.values().stream()
                            .filter(invitation -> invitation.companyId().equals(companyId))
                            .filter(invitation -> invitation.clientId().equals(clientId))
                            .filter(invitation -> invitation.isPendingAt(now))
                            .findFirst();
                }

                @Override
                public Optional<TelegramInvitation> findPendingByIdAndCompanyId(InvitationTokenId invitationId, CompanyId companyId, Instant now) {
                    return Optional.ofNullable(invitations.get(invitationId))
                            .filter(invitation -> invitation.companyId().equals(companyId))
                            .filter(invitation -> invitation.isPendingAt(now));
                }

                @Override
                public Optional<TelegramInvitation> findByTokenHash(String tokenHash) {
                    return invitations.values().stream()
                            .filter(invitation -> invitation.tokenHash().equals(tokenHash))
                            .findFirst();
                }

                @Override
                public Optional<TelegramInvitation> findByTokenHashForUpdate(String tokenHash) {
                    return findByTokenHash(tokenHash);
                }
            };
        }

        private ClientTelegramAccountRepositoryPort accountRepository() {
            return new ClientTelegramAccountRepositoryPort() {
                @Override
                public boolean existsActiveByCompanyIdAndClientId(CompanyId companyId, ClientId clientId) {
                    return findActiveByCompanyIdAndClientId(companyId, clientId).isPresent();
                }

                @Override
                public Optional<ClientTelegramAccount> findActiveByCompanyIdAndClientId(CompanyId companyId, ClientId clientId) {
                    return Optional.ofNullable(accounts.get(clientId))
                            .filter(account -> account.companyId().equals(companyId))
                            .filter(account -> account.status() == TelegramAccountStatus.ACTIVE);
                }

                @Override
                public boolean existsActiveByCompanyIdAndTelegramUserId(CompanyId companyId, long telegramUserId) {
                    return accounts.values().stream()
                            .anyMatch(account -> account.companyId().equals(companyId)
                                    && account.telegramUserId() == telegramUserId
                                    && account.status() == TelegramAccountStatus.ACTIVE);
                }

                @Override
                public ClientTelegramAccount save(ClientTelegramAccount account) {
                    accounts.put(account.clientId(), account);
                    return account;
                }
            };
        }

        private TokenGeneratorPort tokenGenerator() {
            return () -> "raw-token-123";
        }

        private TokenHashingPort tokenHashing() {
            return rawToken -> "hashed-" + rawToken;
        }

        private TelegramInvitationIdGeneratorPort invitationIdGenerator() {
            return () -> INVITATION_ID;
        }

        private TelegramLinkEventIdGeneratorPort eventIdGenerator() {
            return () -> EVENT_ID;
        }

        private TelegramInvitationLinkBuilderPort linkBuilder() {
            return rawToken -> "https://t.me/test_bot?start=" + rawToken;
        }

        private TelegramInvitationAuditPort auditPort() {
            return auditEvents::add;
        }

        private ClockPort clock() {
            return () -> NOW;
        }

        private TransactionRunnerPort transactionRunner() {
            return new TransactionRunnerPort() {
                @Override
                public <T> T inWriteTransaction(Supplier<T> operation) {
                    return operation.get();
                }
            };
        }
    }
}
