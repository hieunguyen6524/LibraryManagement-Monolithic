package com.example.LibraryManagement_Monolithic.repository;

import com.example.LibraryManagement_Monolithic.entity.BlacklistedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, Long> {
    Optional<BlacklistedToken> findByToken(String token);

    boolean existsByToken(String token);
}
