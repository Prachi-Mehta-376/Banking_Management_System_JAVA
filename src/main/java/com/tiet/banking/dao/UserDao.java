package com.tiet.banking.dao;

import com.tiet.banking.dto.CustomerSearchResult;
import com.tiet.banking.model.Role;
import com.tiet.banking.model.User;
import java.util.List;
import java.util.Optional;

public interface UserDao {
    User insert(User user);

    Optional<User> findById(long id);

    Optional<User> findByEmail(String normalizedEmail);

    long countByRole(Role role);

    List<User> findByRole(Role role);

    List<CustomerSearchResult> searchCustomers(String query);

    boolean updateActiveStatus(long userId, boolean active);
}
