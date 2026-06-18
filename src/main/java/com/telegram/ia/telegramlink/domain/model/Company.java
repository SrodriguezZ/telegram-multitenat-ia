package com.telegram.ia.telegramlink.domain.model;

import com.telegram.ia.telegramlink.domain.valueobject.CompanyId;

public record Company(CompanyId id, String name, CompanyStatus status) {
    public boolean active() { return status == CompanyStatus.ACTIVE; }
}
