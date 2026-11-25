package com.tiet.banking.db;

@FunctionalInterface
public interface TransactionWork<T> {
    T execute(BankingUnitOfWork uow);
}
