package com.telegram.ia.telegramlink.infrastructure.transaction;

import com.telegram.ia.telegramlink.application.port.out.TransactionRunnerPort;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SpringTransactionRunnerAdapter implements TransactionRunnerPort {
    @Override
    @Transactional
    public <T> T inWriteTransaction(Supplier<T> operation) {
        return operation.get();
    }
}
