package com.telegram.ia.telegramlink.application.response;

import com.telegram.ia.telegramlink.domain.model.CompanyUser;
import com.telegram.ia.telegramlink.domain.model.CompanyUserRole;
import com.telegram.ia.telegramlink.domain.model.CompanyUserStatus;
import com.telegram.ia.telegramlink.domain.valueobject.CompanyId;
import com.telegram.ia.telegramlink.domain.valueobject.CompanyUserId;

public record AuthenticatedUser(
        CompanyUserId companyUserId,
        CompanyId companyId,
        CompanyUserRole role,
        CompanyUserStatus status) {

    public AuthenticatedUser {
        if (companyUserId == null) throw new IllegalArgumentException("company user id is required");
        if (companyId == null) throw new IllegalArgumentException("company id is required");
        if (role == null) throw new IllegalArgumentException("company user role is required");
        if (status == null) throw new IllegalArgumentException("company user status is required");
    }

    public CompanyUser toCompanyUser() {
        return new CompanyUser(companyUserId, companyId, role, status);
    }
}
