package com.fincopilot.repository;

import com.fincopilot.entity.ChatThread;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public interface ChatThreadRepository extends JpaRepository<ChatThread, UUID> {

    Optional<ChatThread> findByIdAndOrgId(UUID id, UUID orgId);

    List<ChatThread> findByOrgIdOrderByUpdatedAtDesc(UUID orgId);

    List<ChatThread> findByOrgIdAndCreatedByUserIdOrderByUpdatedAtDesc(UUID orgId, UUID userId);
}
