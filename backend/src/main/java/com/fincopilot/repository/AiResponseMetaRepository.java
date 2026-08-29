package com.fincopilot.repository;

import com.fincopilot.entity.AiResponseMeta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;


@Repository
public interface AiResponseMetaRepository extends JpaRepository<AiResponseMeta, UUID> {

   
    Optional<AiResponseMeta> findByChatMessageId(UUID chatMessageId);

   
    Optional<AiResponseMeta> findByRequestId(UUID requestId);
}
