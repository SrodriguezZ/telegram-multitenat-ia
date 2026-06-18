package com.telegram.ia.telegramlink.application.port.out;

import com.telegram.ia.telegramlink.domain.model.TelegramInvitation;
import com.telegram.ia.telegramlink.domain.valueobject.ClientId;
import com.telegram.ia.telegramlink.domain.valueobject.CompanyId;
import com.telegram.ia.telegramlink.domain.valueobject.InvitationTokenId;
import java.time.Instant;
import java.util.Optional;

public interface TelegramInvitationRepositoryPort {
    TelegramInvitation save(TelegramInvitation invitation);
    Optional<TelegramInvitation> findPendingByClientId(CompanyId companyId, ClientId clientId, Instant now);
    Optional<TelegramInvitation> findPendingByIdAndCompanyId(InvitationTokenId invitationId, CompanyId companyId, Instant now);
    Optional<TelegramInvitation> findByTokenHash(String tokenHash);
    Optional<TelegramInvitation> findByTokenHashForUpdate(String tokenHash);
}
