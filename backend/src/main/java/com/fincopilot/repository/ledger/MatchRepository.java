package com.fincopilot.repository.ledger;

import com.fincopilot.entity.ledger.Match;
import com.fincopilot.entity.ledger.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;


@Repository
public interface MatchRepository extends JpaRepository<Match, UUID> {

    List<Match> findByOrgIdAndStatusOrderByCreatedAtDesc(UUID orgId, MatchStatus status);

    List<Match> findByBankTransactionId(UUID bankTransactionId);

    List<Match> findByInvoiceId(UUID invoiceId);

    List<Match> findByJournalEntryId(UUID journalEntryId);

    List<Match> findByOrgIdOrderByCreatedAtDesc(UUID orgId);
}
