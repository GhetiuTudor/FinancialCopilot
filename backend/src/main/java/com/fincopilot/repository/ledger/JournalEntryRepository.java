package com.fincopilot.repository.ledger;

import com.fincopilot.entity.ledger.JournalEntry;
import com.fincopilot.entity.ledger.SourceSystem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, UUID> {

        List<JournalEntry> findByOrgIdAndEntryDateBetweenOrderByEntryDateDesc( UUID orgId, LocalDate startDate, LocalDate endDate);

        Optional<JournalEntry> findByOrgIdAndSourceSystemAndExternalId( UUID orgId, SourceSystem sourceSystem, String externalId);

        List<JournalEntry> findByOrgIdOrderByEntryDateDesc(UUID orgId);

        List<JournalEntry> findBySaftImportId(UUID saftImportId);

        Optional<JournalEntry> findFirstByOrgIdOrderByEntryDateDesc(UUID orgId);

        void deleteAllByOrgId(UUID orgId);
}
