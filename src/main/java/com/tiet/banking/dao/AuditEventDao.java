package com.tiet.banking.dao;

import com.tiet.banking.dto.AuditEventView;
import com.tiet.banking.model.AuditEvent;
import java.util.List;

public interface AuditEventDao {
    AuditEvent insert(AuditEvent event);

    List<AuditEventView> findRecent(int limit, String actionFilter);
}
