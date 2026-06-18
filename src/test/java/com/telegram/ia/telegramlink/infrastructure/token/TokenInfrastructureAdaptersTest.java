package com.telegram.ia.telegramlink.infrastructure.token;

import static org.assertj.core.api.Assertions.assertThat;

import com.telegram.ia.telegramlink.infrastructure.clock.SystemClockAdapter;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TokenInfrastructureAdaptersTest {

    @Test
    void secureTokenGeneratorReturnsUniqueUrlSafeOpaqueTokens() {
        SecureTokenGeneratorAdapter generator = new SecureTokenGeneratorAdapter();

        String first = generator.generateToken();
        String second = generator.generateToken();

        assertThat(first).matches("[A-Za-z0-9_-]{32,}");
        assertThat(second).matches("[A-Za-z0-9_-]{32,}");
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void sha256TokenHashingUsesPepperAndNeverReturnsRawToken() {
        Sha256TokenHashingAdapter pepperA = new Sha256TokenHashingAdapter("pepper-a");
        Sha256TokenHashingAdapter pepperB = new Sha256TokenHashingAdapter("pepper-b");

        String first = pepperA.hash("raw-token");
        String repeated = pepperA.hash("raw-token");
        String differentPepper = pepperB.hash("raw-token");

        assertThat(first).matches("[a-f0-9]{64}");
        assertThat(first).isEqualTo(repeated);
        assertThat(first).isNotEqualTo(differentPepper);
        assertThat(first).doesNotContain("raw-token");
    }

    @Test
    void systemClockAdapterReturnsCurrentInstant() {
        SystemClockAdapter clock = new SystemClockAdapter();
        Instant before = Instant.now().minusSeconds(1);

        Instant now = clock.now();

        assertThat(now).isBetween(before, Instant.now().plusSeconds(1));
    }
}
