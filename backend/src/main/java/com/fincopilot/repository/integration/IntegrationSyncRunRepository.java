package com.fincopilot.repository.integration;

import com.fincopilot.entity.integration.IntegrationProvider;
import com.fincopilot.entity.integration.IntegrationSyncRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;


@Repository
public interface IntegrationSyncRunRepository extends JpaRepository<IntegrationSyncRun, UUID> {

    
    List<IntegrationSyncRun> findTop20ByOrgIdAndProviderOrderByStartedAtDesc(
            UUID orgId, IntegrationProvider provider);
}
