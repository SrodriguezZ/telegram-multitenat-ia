package com.telegram.ia.telegramlink.application.port.out;

import com.telegram.ia.telegramlink.domain.model.Client;
import com.telegram.ia.telegramlink.domain.valueobject.ClientId;
import java.util.Optional;

public interface ClientRepositoryPort {
    Optional<Client> findById(ClientId clientId);
}
