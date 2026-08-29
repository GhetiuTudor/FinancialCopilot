package com.fincopilot.repository.ledger;

import com.fincopilot.entity.ledger.Invoice;
import com.fincopilot.entity.ledger.InvoiceDirection;
import com.fincopilot.entity.ledger.InvoiceStatus;
import com.fincopilot.entity.ledger.SourceSystem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

        List<Invoice> findByOrgIdOrderByIssueDateDesc(UUID orgId);

        List<Invoice> findByOrgIdAndDirectionOrderByIssueDateDesc(UUID orgId, InvoiceDirection direction);

        List<Invoice> findByOrgIdAndStatusOrderByDueDateAsc(UUID orgId, InvoiceStatus status);

        List<Invoice> findByOrgIdAndIssueDateBetweenOrderByIssueDateDesc(UUID orgId, LocalDate startDate, LocalDate endDate);

        Optional<Invoice> findByOrgIdAndSourceSystemAndExternalId(UUID orgId, SourceSystem sourceSystem, String externalId);

        List<Invoice> findByCounterpartyId(UUID counterpartyId);

        List<Invoice> findBySaftImportId(UUID saftImportId);

        Optional<Invoice> findFirstByOrgIdOrderByIssueDateDesc(UUID orgId);

        void deleteAllByOrgId(UUID orgId);
}
