package com.tiet.banking.dao;

import com.tiet.banking.dto.AccountActionRequestView;
import com.tiet.banking.model.AccountActionRequest;
import com.tiet.banking.model.RequestStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AccountActionRequestDao {
    AccountActionRequest insert(AccountActionRequest request);

    Optional<AccountActionRequest> findById(long requestId);

    boolean hasPendingRequestForAccount(long accountId);

    List<AccountActionRequestView> findPendingViews();

    List<AccountActionRequestView> findAllViews(int limit);

    boolean updateDecisionIfPending(long requestId, RequestStatus status, long reviewedByUserId, Instant reviewedAt);
}
