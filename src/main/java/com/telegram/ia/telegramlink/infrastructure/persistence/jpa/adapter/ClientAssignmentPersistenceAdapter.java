package com.telegram.ia.telegramlink.infrastructure.persistence.jpa.adapter;

import com.telegram.ia.telegramlink.application.port.out.ClientAssignmentRepositoryPort;
import com.telegram.ia.telegramlink.domain.valueobject.ClientId;
import com.telegram.ia.telegramlink.domain.valueobject.CompanyId;
import com.telegram.ia.telegramlink.domain.valueobject.CompanyUserId;
import com.telegram.ia.telegramlink.infrastructure.persistence.jpa.repository.CompanyUserClientAssignmentJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public class ClientAssignmentPersistenceAdapter implements ClientAssignmentRepositoryPort {
    private final CompanyUserClientAssignmentJpaRepository repository;

    public ClientAssignmentPersistenceAdapter(CompanyUserClientAssignmentJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existsActiveAssignment(CompanyUserId companyUserId, ClientId clientId, CompanyId companyId) {
        return repository.existsByCompanyUserIdAndClientIdAndCompanyIdAndStatus(
                companyUserId.value(), clientId.value(), companyId.value(), "ACTIVE");
    }
}
