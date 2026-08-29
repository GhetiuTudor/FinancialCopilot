package com.fincopilot.repository.ledger;

import com.fincopilot.entity.ledger.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByOrgIdOrderByCreatedAtDesc(UUID orgId);

    List<Payment> findByOrgIdAndPaymentDateBetweenOrderByPaymentDateDesc(UUID orgId, LocalDate from, LocalDate to);

    List<Payment> findBySaftImportId(UUID saftImportId);

    void deleteAllByOrgId(UUID orgId);
}
