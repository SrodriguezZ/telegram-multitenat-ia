package com.telegram.ia.telegramlink.infrastructure.web.admin;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateTelegramInvitationHttpRequest(@NotNull UUID clientId) {}
