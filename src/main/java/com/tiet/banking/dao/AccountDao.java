package com.tiet.banking.dao;

import com.tiet.banking.dto.AdministrativeSummary;
import com.tiet.banking.model.AccountStatus;
import com.tiet.banking.model.BankAccount;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AccountDao {
    BankAccount createAccount(long userId, Instant createdAt);

    Optional<BankAccount> findById(long accountId);

    Optional<BankAccount> findByUserId(long userId);

    Optional<BankAccount> findByAccountNumber(String accountNumber);

    List<BankAccount> findAllOrderById();

    boolean creditIfActive(long accountId, long positiveDeltaPaise);

    boolean debitIfActiveAndSufficient(long accountId, long positiveDebitPaise);

    boolean updateStatus(long accountId, AccountStatus status);

    AdministrativeSummary getAdministrativeSummary();
}
