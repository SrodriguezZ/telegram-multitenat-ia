package com.telegram.ia.telegramlink.domain.model;

import com.telegram.ia.telegramlink.domain.valueobject.CompanyId;
import com.telegram.ia.telegramlink.domain.valueobject.CompanyUserId;

public record CompanyUser(CompanyUserId id, CompanyId companyId, CompanyUserRole role, CompanyUserStatus status) {
    public boolean active() { return status == CompanyUserStatus.ACTIVE; }
    public boolean managementRole() { return role == CompanyUserRole.OWNER || role == CompanyUserRole.ADMIN || role == CompanyUserRole.SUPERVISOR; }
}
