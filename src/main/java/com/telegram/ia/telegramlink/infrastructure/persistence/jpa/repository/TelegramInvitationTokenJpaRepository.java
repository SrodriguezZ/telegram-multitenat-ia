package com.telegram.ia.telegramlink.infrastructure.persistence.jpa.repository;

import com.telegram.ia.telegramlink.infrastructure.persistence.jpa.entity.TelegramInvitationTokenJpaEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TelegramInvitationTokenJpaRepository extends JpaRepository<TelegramInvitationTokenJpaEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select invitation from TelegramInvitationTokenJpaEntity invitation where invitation.tokenHash = :tokenHash and invitation.status = 'PENDING'")
    Optional<TelegramInvitationTokenJpaEntity> findPendingByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    Optional<TelegramInvitationTokenJpaEntity> findByTokenHash(String tokenHash);

    Optional<TelegramInvitationTokenJpaEntity> findFirstByCompanyIdAndClientIdAndStatusAndExpiresAtAfter(
            UUID companyId, UUID clientId, String status, java.time.Instant now);

    Optional<TelegramInvitationTokenJpaEntity> findByIdAndCompanyIdAndStatusAndExpiresAtAfter(
            UUID id, UUID companyId, String status, java.time.Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select invitation from TelegramInvitationTokenJpaEntity invitation where invitation.tokenHash = :tokenHash")
    Optional<TelegramInvitationTokenJpaEntity> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);
}
