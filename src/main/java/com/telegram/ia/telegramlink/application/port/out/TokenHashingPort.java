package com.telegram.ia.telegramlink.application.port.out;

public interface TokenHashingPort {
    String hash(String rawToken);
}
