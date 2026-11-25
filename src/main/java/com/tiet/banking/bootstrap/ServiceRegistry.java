package com.tiet.banking.bootstrap;

import com.tiet.banking.db.DatabaseManager;
import com.tiet.banking.db.JdbcTransactionExecutor;
import com.tiet.banking.db.TransactionExecutor;
import com.tiet.banking.security.PasswordHasher;
import com.tiet.banking.security.SessionManager;
import com.tiet.banking.service.AccountControlService;
import com.tiet.banking.service.AdminService;
import com.tiet.banking.service.AuthService;
import com.tiet.banking.service.CustomerBankingService;
import com.tiet.banking.service.ReconciliationService;
import com.tiet.banking.service.StaffService;
import com.tiet.banking.service.TellerService;
import com.tiet.banking.util.AppConfig;

public class ServiceRegistry {
    private final AppConfig config;
    private final DatabaseManager databaseManager;
    private final TransactionExecutor transactionExecutor;
    private final PasswordHasher passwordHasher;
    private final SessionManager sessionManager;
    private final AuthService authService;
    private final CustomerBankingService customerBankingService;
    private final TellerService tellerService;
    private final StaffService staffService;
    private final AdminService adminService;
    private final ReconciliationService reconciliationService;
    private final AccountControlService accountControlService;

    public ServiceRegistry(AppConfig config) {
        this.config = config;
        this.databaseManager = new DatabaseManager(config);
        this.databaseManager.initializeSchema();
        this.transactionExecutor = new JdbcTransactionExecutor(databaseManager);
        this.passwordHasher = new PasswordHasher(config.pbkdf2Iterations());
        this.sessionManager = new SessionManager();
        this.authService = new AuthService(transactionExecutor, passwordHasher, sessionManager);
        this.customerBankingService = new CustomerBankingService(transactionExecutor, sessionManager);
        this.tellerService = new TellerService(transactionExecutor, sessionManager);
        this.staffService = new StaffService(transactionExecutor, sessionManager);
        this.adminService = new AdminService(transactionExecutor, passwordHasher, sessionManager);
        this.reconciliationService = new ReconciliationService(transactionExecutor, sessionManager);
        this.accountControlService = new AccountControlService(transactionExecutor, sessionManager);
    }

    public AppConfig config() {
        return config;
    }

    public DatabaseManager databaseManager() {
        return databaseManager;
    }

    public TransactionExecutor transactionExecutor() {
        return transactionExecutor;
    }

    public AuthService authService() {
        return authService;
    }

    public CustomerBankingService customerBankingService() {
        return customerBankingService;
    }

    public TellerService tellerService() {
        return tellerService;
    }

    public StaffService staffService() {
        return staffService;
    }

    public AdminService adminService() {
        return adminService;
    }

    public ReconciliationService reconciliationService() {
        return reconciliationService;
    }

    public AccountControlService accountControlService() {
        return accountControlService;
    }
}
