package com.fincopilot.repository.ledger;

import com.fincopilot.entity.ledger.SaftImport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SaftImportRepository extends JpaRepository<SaftImport, UUID> {

    List<SaftImport> findByOrgIdOrderByImportedAtDesc(UUID orgId);

    void deleteAllByOrgId(UUID orgId);
}
