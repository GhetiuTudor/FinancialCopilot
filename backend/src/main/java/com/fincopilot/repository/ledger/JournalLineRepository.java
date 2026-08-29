package com.fincopilot.repository.ledger;

import com.fincopilot.entity.ledger.JournalLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;


@Repository
public interface JournalLineRepository extends JpaRepository<JournalLine, UUID> {

    List<JournalLine> findByEntryId(UUID entryId);

    List<JournalLine> findByOrgIdAndAccountId(UUID orgId, UUID accountId);

    List<JournalLine> findByOrgIdAndCounterpartyId(UUID orgId, UUID counterpartyId);

    void deleteAllByOrgId(UUID orgId);
}
