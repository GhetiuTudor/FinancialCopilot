package com.fincopilot.repository.ledger;

import com.fincopilot.entity.ledger.InvoiceLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;


@Repository
public interface InvoiceLineRepository extends JpaRepository<InvoiceLine, UUID> {

    List<InvoiceLine> findByInvoiceId(UUID invoiceId);

    void deleteAllByInvoiceOrgId(UUID orgId);
}
