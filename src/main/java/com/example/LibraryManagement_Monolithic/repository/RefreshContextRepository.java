package com.example.LibraryManagement_Monolithic.repository;

import com.example.LibraryManagement_Monolithic.entity.RefreshContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshContextRepository extends JpaRepository<RefreshContext, Long> {
    Optional<RefreshContext> findByContextId(String contextId);
}
