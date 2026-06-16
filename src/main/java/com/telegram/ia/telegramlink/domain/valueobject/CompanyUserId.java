package com.telegram.ia.telegramlink.domain.valueobject;

import java.util.UUID;

public record CompanyUserId(UUID value) {
    public CompanyUserId {
        if (value == null) throw new IllegalArgumentException("company user id is required");
    }
}
