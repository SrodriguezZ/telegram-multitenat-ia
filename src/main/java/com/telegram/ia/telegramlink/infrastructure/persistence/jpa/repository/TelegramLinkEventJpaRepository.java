package com.telegram.ia.telegramlink.infrastructure.persistence.jpa.repository;

import com.telegram.ia.telegramlink.infrastructure.persistence.jpa.entity.TelegramLinkEventJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TelegramLinkEventJpaRepository extends JpaRepository<TelegramLinkEventJpaEntity, UUID> {}
