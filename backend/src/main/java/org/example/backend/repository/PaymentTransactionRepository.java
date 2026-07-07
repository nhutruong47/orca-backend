package org.example.backend.repository;

import org.example.backend.entity.PaymentTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {
    Optional<PaymentTransaction> findByTxnRef(String txnRef);

    @Query("SELECT p FROM PaymentTransaction p WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(p.txnRef) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.status) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<PaymentTransaction> searchPayments(@Param("search") String search, Pageable pageable);
}
