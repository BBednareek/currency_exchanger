package org.learn.currencyexchanger.user.domain;


import java.util.Optional;
import java.util.UUID;

// Odpowiada za operacje od DB dotyczace User.
// Domena definiuje, czego potrzebuje od magazynu danych,
// ale nie wie, czy implementacja jest postgres, jpa, czy test memory

public interface UserRepository{
    Optional<User> findById(UUID userId);
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    User save(User user);
}
