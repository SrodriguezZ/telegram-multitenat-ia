package com.telegram.ia.telegramlink.infrastructure.web.admin;

import java.time.Instant;
import java.util.UUID;

public record TelegramInvitationRevokedHttpResponse(UUID invitationId, String status, Instant revokedAt) {}
