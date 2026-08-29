package com.fincopilot.integration;

import com.fincopilot.entity.integration.IntegrationConnection;
import com.fincopilot.entity.integration.IntegrationProvider;

import java.util.UUID;

//interfata pt adaptoare
public interface IntegrationAdapter {

   
    IntegrationProvider provider();

    //testeaza conexiunea inainte de sync
    void testConnection(IntegrationConnection connection) throws IntegrationException;

    SyncResult runSync(UUID orgId, IntegrationConnection connection) throws IntegrationException;
}
