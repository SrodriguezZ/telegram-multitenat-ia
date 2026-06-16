package com.telegram.ia.telegramlink.infrastructure.security;

import com.telegram.ia.telegramlink.application.error.TelegramLinkingApplicationException;
import com.telegram.ia.telegramlink.application.port.out.CurrentUserProviderPort;
import com.telegram.ia.telegramlink.application.response.AuthenticatedUser;
import com.telegram.ia.telegramlink.domain.valueobject.CompanyId;
import com.telegram.ia.telegramlink.domain.valueobject.CompanyUserId;
import org.springframework.stereotype.Component;

@Component
public class SimulatedCurrentUserProviderAdapter implements CurrentUserProviderPort {
    private final SimulatedCurrentUserProperties properties;

    public SimulatedCurrentUserProviderAdapter(SimulatedCurrentUserProperties properties) {
        this.properties = properties;
    }

    @Override
    public AuthenticatedUser currentUser() {
        if (properties.getCompanyUserId() == null || properties.getCompanyId() == null
                || properties.getRole() == null || properties.getStatus() == null) {
            throw new TelegramLinkingApplicationException(
                    "CURRENT_USER_NOT_AVAILABLE", "Simulated current user is not configured");
        }
        return new AuthenticatedUser(
                new CompanyUserId(properties.getCompanyUserId()),
                new CompanyId(properties.getCompanyId()),
                properties.getRole(),
                properties.getStatus());
    }
}
