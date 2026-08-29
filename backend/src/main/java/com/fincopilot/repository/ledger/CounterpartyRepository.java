package com.fincopilot.repository.ledger;

import com.fincopilot.entity.ledger.Counterparty;
import com.fincopilot.entity.ledger.SourceSystem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public interface CounterpartyRepository extends JpaRepository<Counterparty, UUID> {

    List<Counterparty> findByOrgIdOrderByNameAsc(UUID orgId);

    Optional<Counterparty> findByOrgIdAndTaxId(UUID orgId, String taxId);

    List<Counterparty> findByOrgIdAndNameContainingIgnoreCase(UUID orgId, String name);

    Optional<Counterparty> findByOrgIdAndSourceSystemAndExternalId(UUID orgId, SourceSystem sourceSystem, String externalId);

    void deleteAllByOrgId(UUID orgId);
}
