package com.fincopilot.repository.ledger;

import com.fincopilot.entity.ledger.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, UUID> {

    List<BankAccount> findByOrgId(UUID orgId);

    Optional<BankAccount> findByOrgIdAndIban(UUID orgId, String iban);

    Optional<BankAccount> findByOrgIdAndExternalId(UUID orgId, String externalId);

    List<BankAccount> findByConnectionId(UUID connectionId);
}
