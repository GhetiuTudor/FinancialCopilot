package com.fincopilot.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;


@Entity
@Table(name = "ai_response_meta", indexes = {
        @Index(name = "idx_ai_response_meta_org_created", columnList = "org_id, created_at DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiResponseMeta {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_message_id", nullable = false, unique = true)
    private ChatMessage chatMessage;

    @Column(name = "request_id", nullable = false)
    private UUID requestId;

    @Column(name = "as_of", nullable = false)
    private OffsetDateTime asOf;

    @Column(name = "model", nullable = false)
    private String model;

    @Column(name = "context_hash", nullable = false)
    private String contextHash;

    @Column(name = "evidence", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    @Builder.Default
    private String evidence = "[]";

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (evidence == null) {
            evidence = "[]";
        }
    }
}
