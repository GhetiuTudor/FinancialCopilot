package com.fincopilot.repository;

import com.fincopilot.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;


@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

   
    List<ChatMessage> findByThreadIdOrderByCreatedAtAsc(UUID threadId);
   
    List<ChatMessage> findByThreadIdAndOrgIdOrderByCreatedAtAsc(UUID threadId, UUID orgId);
}
