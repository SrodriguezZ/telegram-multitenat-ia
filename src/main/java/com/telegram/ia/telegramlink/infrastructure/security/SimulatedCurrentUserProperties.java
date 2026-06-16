package com.telegram.ia.telegramlink.infrastructure.security;

import com.telegram.ia.telegramlink.domain.model.CompanyUserRole;
import com.telegram.ia.telegramlink.domain.model.CompanyUserStatus;
import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "telegram-link.current-user")
public class SimulatedCurrentUserProperties {
    private UUID companyUserId;
    private UUID companyId;
    private CompanyUserRole role;
    private CompanyUserStatus status;

    public UUID getCompanyUserId() {
        return companyUserId;
    }

    public void setCompanyUserId(UUID companyUserId) {
        this.companyUserId = companyUserId;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public void setCompanyId(UUID companyId) {
        this.companyId = companyId;
    }

    public CompanyUserRole getRole() {
        return role;
    }

    public void setRole(CompanyUserRole role) {
        this.role = role;
    }

    public CompanyUserStatus getStatus() {
        return status;
    }

    public void setStatus(CompanyUserStatus status) {
        this.status = status;
    }
}
