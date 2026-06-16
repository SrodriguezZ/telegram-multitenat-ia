package com.telegram.ia.telegramlink.domain.model;

import com.telegram.ia.telegramlink.domain.valueobject.ClientId;
import com.telegram.ia.telegramlink.domain.valueobject.CompanyId;

public record Client(ClientId id, CompanyId companyId, String fullName, ClientStatus status) {
    public boolean active() { return status == ClientStatus.ACTIVE; }
}
