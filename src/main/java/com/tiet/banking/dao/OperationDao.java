package com.tiet.banking.dao;

import com.tiet.banking.model.BankOperation;
import java.util.List;
import java.util.Optional;

public interface OperationDao {
    BankOperation insert(BankOperation operation);

    Optional<BankOperation> findByReferenceId(String referenceId);

    List<BankOperation> findAllOrderById();
}
