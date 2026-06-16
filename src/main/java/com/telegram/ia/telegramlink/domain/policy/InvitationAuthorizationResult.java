package com.telegram.ia.telegramlink.domain.policy;

import java.util.Optional;

public record InvitationAuthorizationResult(boolean allowed, Optional<InvitationDenialReason> denialReason) {
    public static InvitationAuthorizationResult permit() { return new InvitationAuthorizationResult(true, Optional.empty()); }
    public static InvitationAuthorizationResult denied(InvitationDenialReason reason) { return new InvitationAuthorizationResult(false, Optional.of(reason)); }
}
