package com.telegram.ia.telegramlink.domain.valueobject;

import java.util.UUID;

public record InvitationTokenId(UUID value) {
    public InvitationTokenId {
        if (value == null) throw new IllegalArgumentException("invitation token id is required");
    }
}
