package com.telegram.ia.telegramlink.infrastructure.persistence.jpa.repository;

import com.telegram.ia.telegramlink.infrastructure.persistence.jpa.entity.CompanyUserClientAssignmentJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyUserClientAssignmentJpaRepository extends JpaRepository<CompanyUserClientAssignmentJpaEntity, UUID> {
    boolean existsByCompanyUserIdAndClientIdAndCompanyIdAndStatus(UUID companyUserId, UUID clientId, UUID companyId, String status);
}
