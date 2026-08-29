package com.fincopilot.repository.ledger;

import com.fincopilot.entity.ledger.TaxCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TaxCodeRepository extends JpaRepository<TaxCode, UUID> {

    List<TaxCode> findByOrgIdOrderByTaxCodeAsc(UUID orgId);

    List<TaxCode> findBySaftImportId(UUID saftImportId);

    void deleteAllByOrgId(UUID orgId);
}
