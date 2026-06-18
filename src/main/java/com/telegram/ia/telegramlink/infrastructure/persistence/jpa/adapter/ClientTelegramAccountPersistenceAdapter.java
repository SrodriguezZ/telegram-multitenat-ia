package com.telegram.ia.telegramlink.infrastructure.persistence.jpa.adapter;

import com.telegram.ia.telegramlink.application.port.out.ClientTelegramAccountRepositoryPort;
import com.telegram.ia.telegramlink.domain.model.ClientTelegramAccount;
import com.telegram.ia.telegramlink.domain.valueobject.ClientId;
import com.telegram.ia.telegramlink.domain.valueobject.CompanyId;
import com.telegram.ia.telegramlink.infrastructure.persistence.jpa.mapper.ClientTelegramAccountJpaMapper;
import com.telegram.ia.telegramlink.infrastructure.persistence.jpa.repository.ClientTelegramAccountJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ClientTelegramAccountPersistenceAdapter implements ClientTelegramAccountRepositoryPort {
    private static final String ACTIVE = "ACTIVE";

    private final ClientTelegramAccountJpaRepository repository;

    public ClientTelegramAccountPersistenceAdapter(ClientTelegramAccountJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existsActiveByCompanyIdAndClientId(CompanyId companyId, ClientId clientId) {
        return repository.existsByCompanyIdAndClientIdAndStatus(companyId.value(), clientId.value(), ACTIVE);
    }

    @Override
    public Optional<ClientTelegramAccount> findActiveByCompanyIdAndClientId(CompanyId companyId, ClientId clientId) {
        return repository.findFirstByCompanyIdAndClientIdAndStatus(companyId.value(), clientId.value(), ACTIVE)
                .map(ClientTelegramAccountJpaMapper::toDomain);
    }

    @Override
    public boolean existsActiveByCompanyIdAndTelegramUserId(CompanyId companyId, long telegramUserId) {
        return repository.existsByCompanyIdAndTelegramUserIdAndStatus(companyId.value(), telegramUserId, ACTIVE);
    }

    @Override
    public ClientTelegramAccount save(ClientTelegramAccount account) {
        return ClientTelegramAccountJpaMapper.toDomain(repository.save(ClientTelegramAccountJpaMapper.toEntity(account)));
    }
}
