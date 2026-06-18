package com.telegram.ia.telegramlink.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.telegram.ia.telegramlink.domain.model.TelegramInvitation;
import com.telegram.ia.telegramlink.domain.model.TelegramProfileSnapshot;
import com.telegram.ia.telegramlink.domain.model.TelegramInvitationStatus;
import com.telegram.ia.telegramlink.domain.valueobject.ClientId;
import com.telegram.ia.telegramlink.domain.valueobject.CompanyId;
import com.telegram.ia.telegramlink.domain.valueobject.CompanyUserId;
import com.telegram.ia.telegramlink.domain.valueobject.InvitationTokenId;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TelegramInvitationTest {

    @Test
    void pendingInvitationStoresHashAndPrefixWithoutExposingRawToken() {
        TelegramInvitation invitation = TelegramInvitation.createPending(
                new InvitationTokenId(UUID.randomUUID()),
                new CompanyId(UUID.randomUUID()),
                new ClientId(UUID.randomUUID()),
                new CompanyUserId(UUID.randomUUID()),
                "sha256-token-hash",
                "K7P9",
                Instant.parse("2026-06-14T10:00:00Z"),
                Instant.parse("2026-06-16T10:00:00Z"));

        assertThat(invitation.status()).isEqualTo(TelegramInvitationStatus.PENDING);
        assertThat(invitation.tokenHash()).isEqualTo("sha256-token-hash");
        assertThat(invitation.tokenPrefix()).isEqualTo("K7P9");
        assertThat(Arrays.stream(TelegramInvitation.class.getMethods()).map(Method::getName))
                .noneMatch(methodName -> methodName.toLowerCase().contains("rawtoken")
                        || methodName.toLowerCase().contains("plaintoken"));
    }

    @Test
    void pendingInvitationIsValidOnlyBeforeExpiration() {
        TelegramInvitation invitation = TelegramInvitation.createPending(
                new InvitationTokenId(UUID.randomUUID()),
                new CompanyId(UUID.randomUUID()),
                new ClientId(UUID.randomUUID()),
                new CompanyUserId(UUID.randomUUID()),
                "hash",
                "ABCD",
                Instant.parse("2026-06-14T10:00:00Z"),
                Instant.parse("2026-06-16T10:00:00Z"));

        assertThat(invitation.isPendingAt(Instant.parse("2026-06-15T10:00:00Z"))).isTrue();
        assertThat(invitation.isPendingAt(Instant.parse("2026-06-16T10:00:00Z"))).isFalse();
    }

    @Test
    void usedInvitationCapturesTelegramUserAndTimestamp() {
        TelegramInvitation invitation = TelegramInvitation.createPending(
                new InvitationTokenId(UUID.randomUUID()),
                new CompanyId(UUID.randomUUID()),
                new ClientId(UUID.randomUUID()),
                new CompanyUserId(UUID.randomUUID()),
                "hash",
                "EFGH",
                Instant.parse("2026-06-14T10:00:00Z"),
                Instant.parse("2026-06-16T10:00:00Z"));

        TelegramInvitation used = invitation.markUsed(
                new TelegramProfileSnapshot(987654321L, 123456789L, "client_user", "Jane", "Doe"),
                Instant.parse("2026-06-15T12:00:00Z"));

        assertThat(used.status()).isEqualTo(TelegramInvitationStatus.USED);
        assertThat(used.usedAt()).contains(Instant.parse("2026-06-15T12:00:00Z"));
        assertThat(used.usedByTelegramUserId()).contains(987654321L);
        assertThat(used.isPendingAt(Instant.parse("2026-06-15T12:01:00Z"))).isFalse();
    }
}
