package com.fincopilot.repository.ledger;

import com.fincopilot.entity.ledger.Account;
import com.fincopilot.entity.ledger.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

//la toate interfetele care extind jpa -> findBy<Field><Operator><And/Or><Field><Operator>OrderBy<Field><Asc/Desc>
@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

    List<Account> findByOrgIdOrderByCodeAsc(UUID orgId);

    List<Account> findByOrgIdAndTypeOrderByCodeAsc(UUID orgId, AccountType type);

    Optional<Account> findByOrgIdAndCode(UUID orgId, String code);

    List<Account> findByOrgIdAndIsActiveTrue(UUID orgId);

    void deleteAllByOrgId(UUID orgId);
}
