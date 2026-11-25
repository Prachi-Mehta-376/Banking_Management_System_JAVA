package com.tiet.banking.db;

import java.util.function.Consumer;

public interface TransactionExecutor {
    <T> T execute(TransactionWork<T> work);

    <T> T executeReadOnly(TransactionWork<T> work);

    default void executeVoid(Consumer<BankingUnitOfWork> work) {
        execute(uow -> {
            work.accept(uow);
            return null;
        });
    }
}
