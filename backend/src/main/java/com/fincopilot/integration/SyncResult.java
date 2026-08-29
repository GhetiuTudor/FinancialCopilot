package com.fincopilot.integration;

import com.fincopilot.entity.integration.LastSyncStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

//resultat pt sync
@Getter
@Builder
@AllArgsConstructor
public class SyncResult {

    //ce a fost synced
    private final Map<String, Object> stats;

    private final LastSyncStatus status;
}
