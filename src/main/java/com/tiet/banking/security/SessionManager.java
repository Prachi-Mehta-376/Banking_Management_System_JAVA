package com.tiet.banking.security;

import com.tiet.banking.dto.UserSession;
import com.tiet.banking.exception.AuthorizationException;
import com.tiet.banking.model.Role;
import com.tiet.banking.util.AppLogger;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

public class SessionManager {
    private static final Logger LOG = AppLogger.getLogger(SessionManager.class);

    private final AtomicReference<UserSession> currentSession = new AtomicReference<>();

    public void setSession(UserSession session) {
        currentSession.set(session);
    }

    public void clear() {
        currentSession.set(null);
    }

    public Optional<UserSession> getCurrentSession() {
        return Optional.ofNullable(currentSession.get());
    }

    public UserSession requireAuthenticated() {
        UserSession session = currentSession.get();
        if (session == null) {
            LOG.warning("Rejected unauthenticated operation attempt.");
            throw new AuthorizationException("Authentication is required to perform this action.");
        }
        return session;
    }

    public UserSession requireRole(Role... allowedRoles) {
        UserSession session = requireAuthenticated();
        for (Role allowed : allowedRoles) {
            if (session.role() == allowed) {
                return session;
            }
        }
        LOG.warning(() -> "Authorization denied for userId=" + session.userId()
                + " role=" + session.role() + " required=" + Arrays.toString(allowedRoles));
        throw new AuthorizationException("You do not have permission to perform this action.");
    }
}
