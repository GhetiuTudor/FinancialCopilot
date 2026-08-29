package com.fincopilot.repository.ledger;

import com.fincopilot.entity.ledger.BankTransaction;
import com.fincopilot.entity.ledger.SourceSystem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BankTransactionRepository extends JpaRepository<BankTransaction, UUID> {

        List<BankTransaction> findByOrgIdAndBookingDateBetweenOrderByBookingDateDesc(UUID orgId, LocalDate startDate,
                        LocalDate endDate);

        List<BankTransaction> findByAccountIdOrderByBookingDateDesc(UUID accountId);

        Optional<BankTransaction> findByOrgIdAndAccountIdAndSourceSystemAndExternalId(UUID orgId, UUID accountId,
                        SourceSystem sourceSystem, String externalId);
}
