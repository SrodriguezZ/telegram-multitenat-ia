package com.telegram.ia.telegramlink.infrastructure.persistence.jpa.repository;

import com.telegram.ia.telegramlink.infrastructure.persistence.jpa.entity.CompanyJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyJpaRepository extends JpaRepository<CompanyJpaEntity, UUID> {}
