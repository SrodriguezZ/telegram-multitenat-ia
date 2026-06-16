package com.telegram.ia.telegramlink.application.port.out;

import com.telegram.ia.telegramlink.domain.model.ClientTelegramAccount;
import com.telegram.ia.telegramlink.domain.valueobject.ClientId;
import com.telegram.ia.telegramlink.domain.valueobject.CompanyId;
import java.util.Optional;

public interface ClientTelegramAccountRepositoryPort {
    boolean existsActiveByCompanyIdAndClientId(CompanyId companyId, ClientId clientId);
    Optional<ClientTelegramAccount> findActiveByCompanyIdAndClientId(CompanyId companyId, ClientId clientId);
    boolean existsActiveByCompanyIdAndTelegramUserId(CompanyId companyId, long telegramUserId);
    ClientTelegramAccount save(ClientTelegramAccount account);
}
