package com.telegram.ia.telegramlink.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import com.telegram.ia.telegramlink.application.command.ConfirmTelegramLinkCommand;
import com.telegram.ia.telegramlink.application.command.ValidateTelegramLinkTokenCommand;
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
import com.telegram.ia.telegramlink.application.response.TelegramLinkConfirmationStatus;
import com.telegram.ia.telegramlink.application.response.TelegramLinkTokenValidationResponse;
import com.telegram.ia.telegramlink.application.response.TelegramLinkTokenValidationStatus;
import com.telegram.ia.telegramlink.application.usecase.ConfirmTelegramLinkUseCase;
import com.telegram.ia.telegramlink.application.usecase.ValidateTelegramLinkTokenUseCase;
import com.telegram.ia.telegramlink.domain.model.Client;
import com.telegram.ia.telegramlink.domain.model.ClientStatus;
import com.telegram.ia.telegramlink.domain.model.ClientTelegramAccount;
import com.telegram.ia.telegramlink.domain.model.TelegramAccountStatus;
import com.telegram.ia.telegramlink.domain.model.TelegramInvitation;
import com.telegram.ia.telegramlink.domain.model.TelegramLinkEvent;
import com.telegram.ia.telegramlink.domain.model.TelegramLinkEventResult;
import com.telegram.ia.telegramlink.domain.model.TelegramLinkEventType;
import com.telegram.ia.telegramlink.domain.model.TelegramProfileSnapshot;
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

class BotTelegramLinkUseCasesTest {
    private static final Instant NOW = Instant.parse("2026-06-14T12:00:00Z");
    private static final CompanyId COMPANY_ID = new CompanyId(UUID.fromString("10000000-0000-0000-0000-000000000001"));
    private static final CompanyUserId CREATOR_ID = new CompanyUserId(UUID.fromString("10000000-0000-0000-0000-000000000002"));
    private static final ClientId CLIENT_ID = new ClientId(UUID.fromString("10000000-0000-0000-0000-000000000003"));
    private static final InvitationTokenId INVITATION_ID = new InvitationTokenId(UUID.fromString("10000000-0000-0000-0000-000000000004"));
    private static final TelegramAccountId ACCOUNT_ID = new TelegramAccountId(UUID.fromString("10000000-0000-0000-0000-000000000005"));
    private static final TelegramLinkEventId EVENT_ID = new TelegramLinkEventId(UUID.fromString("10000000-0000-0000-0000-000000000006"));
    private static final String RAW_TOKEN = "safe-raw-token";
    private static final long TELEGRAM_USER_ID = 777_001L;

    @Test
    void invalidTokenValidationReturnsInvalidPayloadAndAuditsSafely() {
        Scenario scenario = new Scenario();

        TelegramLinkTokenValidationResponse response = scenario.validateUseCase()
                .execute(new ValidateTelegramLinkTokenCommand("unknown-token", TELEGRAM_USER_ID));

        assertThat(response.status()).isEqualTo(TelegramLinkTokenValidationStatus.INVALID);
        assertThat(response.confirmationRequired()).isFalse();
        assertThat(response.errorCode()).contains("INVALID_TOKEN");
        assertThat(response.clientId()).isEmpty();
        assertThat(scenario.auditEvents)
                .extracting(TelegramLinkEvent::eventType, TelegramLinkEvent::result, TelegramLinkEvent::reasonCode)
                .containsExactly(tuple(TelegramLinkEventType.TOKEN_VALIDATION_FAILED, TelegramLinkEventResult.FAILURE, "INVALID_TOKEN"));
        assertThat(scenario.auditEvents.get(0).metadata()).doesNotContain("unknown-token");
    }

    @Test
    void validTokenValidationPreviewsClientWithoutLinking() {
        Scenario scenario = new Scenario();
        scenario.clients.put(CLIENT_ID, activeClient());
        scenario.invitations.put(INVITATION_ID, pendingInvitation());

        TelegramLinkTokenValidationResponse response = scenario.validateUseCase()
                .execute(new ValidateTelegramLinkTokenCommand(RAW_TOKEN, TELEGRAM_USER_ID));

        assertThat(response.status()).isEqualTo(TelegramLinkTokenValidationStatus.VALID);
        assertThat(response.confirmationRequired()).isTrue();
        assertThat(response.errorCode()).isEmpty();
        assertThat(response.clientId()).contains(CLIENT_ID);
        assertThat(response.clientFullName()).contains("Jane Doe");
        assertThat(response.expiresAt()).contains(NOW.plusSeconds(172_800));
        assertThat(scenario.accounts).isEmpty();
        assertThat(scenario.auditEvents)
                .extracting(TelegramLinkEvent::eventType, TelegramLinkEvent::result, TelegramLinkEvent::reasonCode)
                .containsExactly(tuple(TelegramLinkEventType.TOKEN_VALIDATION_STARTED, TelegramLinkEventResult.SUCCESS, "TOKEN_VALID"));
    }

    @Test
    void duplicateTelegramAccountValidationReturnsInvalidPayloadBeforeConfirmation() {
        Scenario scenario = new Scenario();
        scenario.clients.put(CLIENT_ID, activeClient());
        scenario.invitations.put(INVITATION_ID, pendingInvitation());
        scenario.accounts.put(new ClientId(UUID.fromString("10000000-0000-0000-0000-000000000099")), activeAccountForTelegramUser(TELEGRAM_USER_ID));

        TelegramLinkTokenValidationResponse response = scenario.validateUseCase()
                .execute(new ValidateTelegramLinkTokenCommand(RAW_TOKEN, TELEGRAM_USER_ID));

        assertThat(response.status()).isEqualTo(TelegramLinkTokenValidationStatus.INVALID);
        assertThat(response.errorCode()).contains("TELEGRAM_ACCOUNT_ALREADY_LINKED");
        assertThat(scenario.auditEvents)
                .extracting(TelegramLinkEvent::eventType, TelegramLinkEvent::result, TelegramLinkEvent::reasonCode)
                .containsExactly(tuple(TelegramLinkEventType.DUPLICATE_TELEGRAM_ACCOUNT_DETECTED, TelegramLinkEventResult.FAILURE, "TELEGRAM_ACCOUNT_ALREADY_LINKED"));
    }

    @Test
    void confirmationCreatesPermanentLinkMarksInvitationUsedAndAuditsWithoutRawToken() {
        Scenario scenario = new Scenario();
        scenario.clients.put(CLIENT_ID, activeClient());
        scenario.invitations.put(INVITATION_ID, pendingInvitation());

        ConfirmTelegramLinkResponse response = scenario.confirmUseCase()
                .execute(new ConfirmTelegramLinkCommand(RAW_TOKEN, telegramProfile()));

        TelegramInvitation usedInvitation = scenario.invitations.get(INVITATION_ID);
        ClientTelegramAccount account = scenario.accounts.get(CLIENT_ID);
        assertThat(response.status()).isEqualTo(TelegramLinkConfirmationStatus.CONFIRMED);
        assertThat(response.errorCode()).isEmpty();
        assertThat(response.clientId()).contains(CLIENT_ID);
        assertThat(account.id()).isEqualTo(ACCOUNT_ID);
        assertThat(account.linkedByInvitationTokenId()).contains(INVITATION_ID);
        assertThat(account.telegramUserId()).isEqualTo(TELEGRAM_USER_ID);
        assertThat(usedInvitation.usedAt()).contains(NOW);
        assertThat(usedInvitation.usedByTelegramUserId()).contains(TELEGRAM_USER_ID);
        assertThat(scenario.lockedHashLookups).isEqualTo(1);
        assertThat(scenario.auditEvents)
                .extracting(TelegramLinkEvent::eventType, TelegramLinkEvent::result, TelegramLinkEvent::reasonCode)
                .containsExactly(tuple(TelegramLinkEventType.CLIENT_TELEGRAM_LINKED, TelegramLinkEventResult.SUCCESS, "CLIENT_TELEGRAM_LINKED"));
        assertThat(scenario.auditEvents.get(0).metadata()).doesNotContain(RAW_TOKEN);
    }

    @Test
    void secondConfirmationUsesLockPathAndReturnsSingleUseConflictPayload() {
        Scenario scenario = new Scenario();
        scenario.clients.put(CLIENT_ID, activeClient());
        scenario.invitations.put(INVITATION_ID, pendingInvitation());

        ConfirmTelegramLinkResponse first = scenario.confirmUseCase()
                .execute(new ConfirmTelegramLinkCommand(RAW_TOKEN, telegramProfile()));
        ConfirmTelegramLinkResponse second = scenario.confirmUseCase()
                .execute(new ConfirmTelegramLinkCommand(RAW_TOKEN, telegramProfile()));

        assertThat(first.status()).isEqualTo(TelegramLinkConfirmationStatus.CONFIRMED);
        assertThat(second.status()).isEqualTo(TelegramLinkConfirmationStatus.INVALID);
        assertThat(second.errorCode()).contains("INVITATION_ALREADY_USED");
        assertThat(scenario.accounts).hasSize(1);
        assertThat(scenario.lockedHashLookups).isEqualTo(2);
        assertThat(scenario.auditEvents)
                .extracting(TelegramLinkEvent::eventType, TelegramLinkEvent::result, TelegramLinkEvent::reasonCode)
                .containsExactly(
                        tuple(TelegramLinkEventType.CLIENT_TELEGRAM_LINKED, TelegramLinkEventResult.SUCCESS, "CLIENT_TELEGRAM_LINKED"),
                        tuple(TelegramLinkEventType.CLIENT_TELEGRAM_LINKED, TelegramLinkEventResult.FAILURE, "INVITATION_ALREADY_USED"));
    }

    private static Client activeClient() {
        return new Client(CLIENT_ID, COMPANY_ID, "Jane Doe", ClientStatus.ACTIVE);
    }

    private static TelegramInvitation pendingInvitation() {
        return TelegramInvitation.createPending(
                INVITATION_ID,
                COMPANY_ID,
                CLIENT_ID,
                CREATOR_ID,
                "hashed-" + RAW_TOKEN,
                "safe",
                NOW.minusSeconds(60),
                NOW.plusSeconds(172_800));
    }

    private static TelegramProfileSnapshot telegramProfile() {
        return new TelegramProfileSnapshot(TELEGRAM_USER_ID, 888_002L, "jane_telegram", "Jane", "Telegram");
    }

    private static ClientTelegramAccount activeAccountForTelegramUser(long telegramUserId) {
        ClientId linkedClientId = new ClientId(UUID.fromString("10000000-0000-0000-0000-000000000098"));
        return new ClientTelegramAccount(
                new TelegramAccountId(UUID.fromString("10000000-0000-0000-0000-000000000097")),
                COMPANY_ID,
                linkedClientId,
                telegramUserId,
                888_002L,
                "existing_user",
                "Existing",
                "User",
                Optional.empty(),
                TelegramAccountStatus.ACTIVE,
                NOW.minusSeconds(300),
                Optional.empty(),
                NOW.minusSeconds(300),
                NOW.minusSeconds(300));
    }

    private static final class Scenario {
        private final Map<InvitationTokenId, TelegramInvitation> invitations = new HashMap<>();
        private final Map<ClientId, Client> clients = new HashMap<>();
        private final Map<ClientId, ClientTelegramAccount> accounts = new HashMap<>();
        private final List<TelegramLinkEvent> auditEvents = new ArrayList<>();
        private int lockedHashLookups;

        ValidateTelegramLinkTokenUseCase validateUseCase() {
            return new ValidateTelegramLinkTokenUseCase(invitationRepository(), clientRepository(), accountRepository(), tokenHashing(), eventIdGenerator(), auditPort(), clock());
        }

        ConfirmTelegramLinkUseCase confirmUseCase() {
            return new ConfirmTelegramLinkUseCase(invitationRepository(), clientRepository(), accountRepository(), tokenHashing(), accountIdGenerator(), eventIdGenerator(), auditPort(), clock(), transactionRunner());
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
                    return findByHash(tokenHash);
                }

                @Override
                public Optional<TelegramInvitation> findByTokenHashForUpdate(String tokenHash) {
                    lockedHashLookups++;
                    return findByHash(tokenHash);
                }

                private Optional<TelegramInvitation> findByHash(String tokenHash) {
                    return invitations.values().stream()
                            .filter(invitation -> invitation.tokenHash().equals(tokenHash))
                            .findFirst();
                }
            };
        }

        private ClientRepositoryPort clientRepository() {
            return clientId -> Optional.ofNullable(clients.get(clientId));
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

        private TokenHashingPort tokenHashing() {
            return rawToken -> "hashed-" + rawToken;
        }

        private TelegramAccountIdGeneratorPort accountIdGenerator() {
            return () -> ACCOUNT_ID;
        }

        private TelegramLinkEventIdGeneratorPort eventIdGenerator() {
            return () -> EVENT_ID;
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
