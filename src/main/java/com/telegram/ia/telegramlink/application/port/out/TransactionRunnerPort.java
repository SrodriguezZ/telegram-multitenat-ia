package com.telegram.ia.telegramlink.application.port.out;

import java.util.function.Supplier;

public interface TransactionRunnerPort {
    <T> T inWriteTransaction(Supplier<T> operation);
}
