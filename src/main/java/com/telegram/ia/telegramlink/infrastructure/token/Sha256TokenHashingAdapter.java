package com.telegram.ia.telegramlink.infrastructure.token;

import com.telegram.ia.telegramlink.application.port.out.TokenHashingPort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Sha256TokenHashingAdapter implements TokenHashingPort {
    private final String pepper;

    public Sha256TokenHashingAdapter(@Value("${telegram-link.token.pepper}") String pepper) {
        if (pepper == null || pepper.isBlank()) {
            throw new IllegalArgumentException("telegram-link token pepper is required");
        }
        this.pepper = pepper;
    }

    @Override
    public String hash(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("raw token is required");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest((pepper + rawToken).getBytes(StandardCharsets.UTF_8));
            return toHex(hashed);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 hashing is not available", exception);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte current : bytes) {
            builder.append(String.format("%02x", current));
        }
        return builder.toString();
    }
}
