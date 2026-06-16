package com.telegram.ia.telegramlink.application.port.out;

import java.time.Instant;

public interface ClockPort {
    Instant now();
}
