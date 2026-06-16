package com.telegram.ia.telegramlink.infrastructure.persistence.jpa.adapter;

import com.telegram.ia.telegramlink.application.port.out.TelegramInvitationRepositoryPort;
import com.telegram.ia.telegramlink.domain.model.TelegramInvitation;
import com.telegram.ia.telegramlink.domain.model.TelegramInvitationStatus;
import com.telegram.ia.telegramlink.domain.valueobject.ClientId;
import com.telegram.ia.telegramlink.domain.valueobject.CompanyId;
import com.telegram.ia.telegramlink.domain.valueobject.InvitationTokenId;
import com.telegram.ia.telegramlink.infrastructure.persistence.jpa.mapper.TelegramInvitationJpaMapper;
import com.telegram.ia.telegramlink.infrastructure.persistence.jpa.repository.TelegramInvitationTokenJpaRepository;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class TelegramInvitationPersistenceAdapter implements TelegramInvitationRepositoryPort {
    private final TelegramInvitationTokenJpaRepository repository;

    public TelegramInvitationPersistenceAdapter(TelegramInvitationTokenJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public TelegramInvitation save(TelegramInvitation invitation) {
        return TelegramInvitationJpaMapper.toDomain(repository.save(TelegramInvitationJpaMapper.toEntity(invitation)));
    }

    @Override
    public Optional<TelegramInvitation> findPendingByClientId(CompanyId companyId, ClientId clientId, Instant now) {
        return repository.findFirstByCompanyIdAndClientIdAndStatusAndExpiresAtAfter(
                        companyId.value(), clientId.value(), TelegramInvitationStatus.PENDING.name(), now)
                .map(TelegramInvitationJpaMapper::toDomain);
    }

    @Override
    public Optional<TelegramInvitation> findPendingByIdAndCompanyId(InvitationTokenId invitationId, CompanyId companyId, Instant now) {
        return repository.findByIdAndCompanyIdAndStatusAndExpiresAtAfter(
                        invitationId.value(), companyId.value(), TelegramInvitationStatus.PENDING.name(), now)
                .map(TelegramInvitationJpaMapper::toDomain);
    }

    @Override
    public Optional<TelegramInvitation> findByTokenHash(String tokenHash) {
        return repository.findByTokenHash(tokenHash).map(TelegramInvitationJpaMapper::toDomain);
    }

    public Optional<TelegramInvitation> findPendingByTokenHashForUpdate(String tokenHash) {
        return repository.findPendingByTokenHashForUpdate(tokenHash).map(TelegramInvitationJpaMapper::toDomain);
    }

    @Override
    public Optional<TelegramInvitation> findByTokenHashForUpdate(String tokenHash) {
        return repository.findByTokenHashForUpdate(tokenHash).map(TelegramInvitationJpaMapper::toDomain);
    }
}
