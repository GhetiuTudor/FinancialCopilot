package com.fincopilot.repository.integration;

import com.fincopilot.entity.integration.IntegrationConnection;
import com.fincopilot.entity.integration.IntegrationProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

//la toate interfetele care extind jpa -> findBy<Field><Operator><And/Or><Field><Operator>OrderBy<Field><Asc/Desc>

@Repository
public interface IntegrationConnectionRepository extends JpaRepository<IntegrationConnection, UUID> {

    
    Optional<IntegrationConnection> findByOrgIdAndProvider(UUID orgId, IntegrationProvider provider);

    
    List<IntegrationConnection> findAllByOrgId(UUID orgId);
}
