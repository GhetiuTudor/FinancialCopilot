package com.fincopilot.repository.ledger;

import com.fincopilot.entity.ledger.BankConnection;
import com.fincopilot.entity.ledger.ConnectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;


@Repository
public interface BankConnectionRepository extends JpaRepository<BankConnection, UUID> {

    List<BankConnection> findByOrgId(UUID orgId);

    List<BankConnection> findByOrgIdAndStatus(UUID orgId, ConnectionStatus status);

    List<BankConnection> findByOrgIdAndProvider(UUID orgId, String provider);
}
