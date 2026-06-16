package com.telegram.ia.telegramlink.domain.valueobject;

import java.util.UUID;

public record CompanyId(UUID value) {
    public CompanyId {
        if (value == null) throw new IllegalArgumentException("company id is required");
    }
}
