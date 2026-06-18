package com.telegram.ia.telegramlink.infrastructure.persistence.jpa.repository;

import com.telegram.ia.telegramlink.infrastructure.persistence.jpa.entity.ClientTelegramAccountJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientTelegramAccountJpaRepository extends JpaRepository<ClientTelegramAccountJpaEntity, UUID> {
    boolean existsByCompanyIdAndClientIdAndStatus(UUID companyId, UUID clientId, String status);

    boolean existsByCompanyIdAndTelegramUserIdAndStatus(UUID companyId, Long telegramUserId, String status);

    Optional<ClientTelegramAccountJpaEntity> findFirstByCompanyIdAndClientIdAndStatus(UUID companyId, UUID clientId, String status);
}
