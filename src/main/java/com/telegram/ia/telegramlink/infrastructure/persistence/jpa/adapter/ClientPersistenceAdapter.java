package com.telegram.ia.telegramlink.infrastructure.persistence.jpa.adapter;

import com.telegram.ia.telegramlink.application.port.out.ClientRepositoryPort;
import com.telegram.ia.telegramlink.domain.model.Client;
import com.telegram.ia.telegramlink.domain.model.ClientStatus;
import com.telegram.ia.telegramlink.domain.valueobject.ClientId;
import com.telegram.ia.telegramlink.domain.valueobject.CompanyId;
import com.telegram.ia.telegramlink.infrastructure.persistence.jpa.repository.ClientJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ClientPersistenceAdapter implements ClientRepositoryPort {
    private final ClientJpaRepository repository;

    public ClientPersistenceAdapter(ClientJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Client> findById(ClientId clientId) {
        return repository.findById(clientId.value())
                .map(entity -> new Client(
                        new ClientId(entity.getId()),
                        new CompanyId(entity.getCompanyId()),
                        entity.getFullName(),
                        ClientStatus.valueOf(entity.getStatus())));
    }
}
