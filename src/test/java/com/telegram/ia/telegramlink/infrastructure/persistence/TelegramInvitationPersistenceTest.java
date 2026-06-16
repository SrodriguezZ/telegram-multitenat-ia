package com.telegram.ia.telegramlink.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.telegram.ia.telegramlink.domain.model.TelegramInvitation;
import com.telegram.ia.telegramlink.domain.model.TelegramInvitationStatus;
import com.telegram.ia.telegramlink.domain.valueobject.ClientId;
import com.telegram.ia.telegramlink.domain.valueobject.CompanyId;
import com.telegram.ia.telegramlink.domain.valueobject.CompanyUserId;
import com.telegram.ia.telegramlink.domain.valueobject.InvitationTokenId;
import com.telegram.ia.telegramlink.infrastructure.persistence.jpa.entity.ClientJpaEntity;
import com.telegram.ia.telegramlink.infrastructure.persistence.jpa.entity.ClientTelegramAccountJpaEntity;
import com.telegram.ia.telegramlink.infrastructure.persistence.jpa.entity.CompanyJpaEntity;
import com.telegram.ia.telegramlink.infrastructure.persistence.jpa.entity.CompanyUserClientAssignmentJpaEntity;
import com.telegram.ia.telegramlink.infrastructure.persistence.jpa.entity.CompanyUserJpaEntity;
import com.telegram.ia.telegramlink.infrastructure.persistence.jpa.entity.TelegramInvitationTokenJpaEntity;
import com.telegram.ia.telegramlink.infrastructure.persistence.jpa.adapter.TelegramInvitationPersistenceAdapter;
import com.telegram.ia.telegramlink.infrastructure.persistence.jpa.entity.TelegramLinkEventJpaEntity;
import com.telegram.ia.telegramlink.infrastructure.persistence.jpa.mapper.TelegramInvitationJpaMapper;
import com.telegram.ia.telegramlink.infrastructure.persistence.jpa.repository.ClientJpaRepository;
import com.telegram.ia.telegramlink.infrastructure.persistence.jpa.repository.ClientTelegramAccountJpaRepository;
import com.telegram.ia.telegramlink.infrastructure.persistence.jpa.repository.CompanyJpaRepository;
import com.telegram.ia.telegramlink.infrastructure.persistence.jpa.repository.CompanyUserJpaRepository;
import com.telegram.ia.telegramlink.infrastructure.persistence.jpa.repository.TelegramInvitationTokenJpaRepository;
import jakarta.persistence.Table;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

@SpringBootTest
@Transactional
class TelegramInvitationPersistenceTest {

    @Autowired CompanyJpaRepository companyRepository;
    @Autowired CompanyUserJpaRepository companyUserRepository;
    @Autowired ClientJpaRepository clientRepository;
    @Autowired ClientTelegramAccountJpaRepository telegramAccountRepository;
    @Autowired TelegramInvitationTokenJpaRepository invitationRepository;
    @Autowired TelegramInvitationPersistenceAdapter invitationAdapter;

    @Test
    void mvpEntitiesUseExpectedTableNames() {
        assertThat(tableName(CompanyJpaEntity.class)).isEqualTo("companies");
        assertThat(tableName(CompanyUserJpaEntity.class)).isEqualTo("company_users");
        assertThat(tableName(ClientJpaEntity.class)).isEqualTo("clients");
        assertThat(tableName(CompanyUserClientAssignmentJpaEntity.class)).isEqualTo("company_user_client_assignments");
        assertThat(tableName(TelegramInvitationTokenJpaEntity.class)).isEqualTo("telegram_invitation_tokens");
        assertThat(tableName(ClientTelegramAccountJpaEntity.class)).isEqualTo("client_telegram_accounts");
        assertThat(tableName(TelegramLinkEventJpaEntity.class)).isEqualTo("telegram_link_events");
    }

    @Test
    void invitationMapperPreservesTokenHashAndPrefixWithoutRawToken() {
        Instant now = Instant.parse("2026-06-14T10:00:00Z");
        TelegramInvitation invitation = TelegramInvitation.createPending(
                new InvitationTokenId(UUID.randomUUID()),
                new CompanyId(UUID.randomUUID()),
                new ClientId(UUID.randomUUID()),
                new CompanyUserId(UUID.randomUUID()),
                "sha256-hash",
                "K7P9",
                now,
                now.plusSeconds(172800));

        TelegramInvitationTokenJpaEntity entity = TelegramInvitationJpaMapper.toEntity(invitation);
        TelegramInvitation restored = TelegramInvitationJpaMapper.toDomain(entity);

        assertThat(entity.getTokenHash()).isEqualTo("sha256-hash");
        assertThat(entity.getTokenPrefix()).isEqualTo("K7P9");
        assertThat(restored.tokenHash()).isEqualTo("sha256-hash");
        assertThat(restored.tokenPrefix()).isEqualTo("K7P9");
    }

    @Test
    void repositoryCanFindPendingInvitationWithWriteLockByTokenHash() {
        UUID companyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        Instant now = Instant.parse("2026-06-14T10:00:00Z");
        companyRepository.save(new CompanyJpaEntity(companyId, "Company", null, "ACTIVE", now, now));
        companyUserRepository.save(new CompanyUserJpaEntity(userId, companyId, "agent@example.com", "Agent", "AGENT", "ACTIVE", now, now));
        clientRepository.save(new ClientJpaEntity(clientId, companyId, null, null, null, "Client", null, null, "ACTIVE", now, now));
        invitationRepository.save(new TelegramInvitationTokenJpaEntity(
                UUID.randomUUID(), companyId, clientId, userId, "sha256-hash", "K7P9", "PENDING",
                now.plusSeconds(172800), null, null, null, null, now, now));

        assertThat(invitationRepository.findPendingByTokenHashForUpdate("sha256-hash"))
                .isPresent()
                .get()
                .extracting(TelegramInvitationTokenJpaEntity::getStatus)
                .isEqualTo(TelegramInvitationStatus.PENDING.name());
        assertThat(invitationAdapter.findPendingByTokenHashForUpdate("sha256-hash"))
                .isPresent()
                .get()
                .extracting(TelegramInvitation::tokenHash)
                .isEqualTo("sha256-hash");
    }

    @Test
    void activeTelegramAccountsAreUniqueByCompanyTelegramUserAndCompanyClient() {
        UUID companyId = UUID.randomUUID();
        UUID firstClientId = UUID.randomUUID();
        UUID secondClientId = UUID.randomUUID();
        Instant now = Instant.parse("2026-06-14T10:00:00Z");
        companyRepository.save(new CompanyJpaEntity(companyId, "Company", null, "ACTIVE", now, now));
        clientRepository.save(new ClientJpaEntity(firstClientId, companyId, null, null, null, "First Client", null, null, "ACTIVE", now, now));
        clientRepository.save(new ClientJpaEntity(secondClientId, companyId, null, null, null, "Second Client", null, null, "ACTIVE", now, now));

        telegramAccountRepository.saveAndFlush(new ClientTelegramAccountJpaEntity(
                UUID.randomUUID(), companyId, firstClientId, 777_001L, 888_001L, "first_user", "First", "User",
                null, "ACTIVE", now, null, now, now));

        assertThatThrownBy(() -> telegramAccountRepository.saveAndFlush(new ClientTelegramAccountJpaEntity(
                UUID.randomUUID(), companyId, secondClientId, 777_001L, 888_002L, "second_user", "Second", "User",
                null, "ACTIVE", now, null, now, now)))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> telegramAccountRepository.saveAndFlush(new ClientTelegramAccountJpaEntity(
                UUID.randomUUID(), companyId, firstClientId, 777_002L, 888_003L, "third_user", "Third", "User",
                null, "ACTIVE", now, null, now, now)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private static String tableName(Class<?> entityClass) {
        return entityClass.getAnnotation(Table.class).name();
    }
}
