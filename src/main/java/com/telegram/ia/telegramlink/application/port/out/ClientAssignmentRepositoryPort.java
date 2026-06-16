package com.telegram.ia.telegramlink.application.port.out;

import com.telegram.ia.telegramlink.domain.valueobject.ClientId;
import com.telegram.ia.telegramlink.domain.valueobject.CompanyId;
import com.telegram.ia.telegramlink.domain.valueobject.CompanyUserId;

public interface ClientAssignmentRepositoryPort {
    boolean existsActiveAssignment(CompanyUserId companyUserId, ClientId clientId, CompanyId companyId);
}
