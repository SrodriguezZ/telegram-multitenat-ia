package com.telegram.ia.telegramlink.infrastructure.persistence.jpa.adapter;

import com.telegram.ia.telegramlink.application.port.out.TelegramInvitationAuditPort;
import com.telegram.ia.telegramlink.domain.model.TelegramLinkEvent;
import com.telegram.ia.telegramlink.infrastructure.persistence.jpa.mapper.TelegramLinkEventJpaMapper;
import com.telegram.ia.telegramlink.infrastructure.persistence.jpa.repository.TelegramLinkEventJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public class TelegramLinkEventPersistenceAdapter implements TelegramInvitationAuditPort {
    private final TelegramLinkEventJpaRepository repository;

    public TelegramLinkEventPersistenceAdapter(TelegramLinkEventJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void record(TelegramLinkEvent event) {
        repository.save(TelegramLinkEventJpaMapper.toEntity(event));
    }
}
