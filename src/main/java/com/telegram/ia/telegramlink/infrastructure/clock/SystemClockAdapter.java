package com.telegram.ia.telegramlink.infrastructure.clock;

import com.telegram.ia.telegramlink.application.port.out.ClockPort;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class SystemClockAdapter implements ClockPort {
    @Override
    public Instant now() {
        return Instant.now();
    }
}
