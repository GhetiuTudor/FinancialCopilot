package com.fincopilot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fincopilot.ai.context.FinancialContext;
import com.fincopilot.ai.context.LedgerContextPackBuilder;
import com.fincopilot.dto.ai.*;
import com.fincopilot.dto.ai.AiChatTurnDto;
import com.fincopilot.dto.chat.*;
import com.fincopilot.entity.AiResponseMeta;
import com.fincopilot.entity.ChatMessage;
import com.fincopilot.entity.ChatMessage.MessageRole;
import com.fincopilot.entity.ChatThread;
import com.fincopilot.repository.AiResponseMetaRepository;
import com.fincopilot.repository.ChatMessageRepository;
import com.fincopilot.repository.ChatThreadRepository;
import com.fincopilot.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

//operatii pe thread cu mesaje 
//corelation id trebuie pasat in jos pe chain 
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

        private final ChatThreadRepository threadRepository;
        private final ChatMessageRepository messageRepository;
        private final AiResponseMetaRepository aiResponseMetaRepository;
        private final WebClient aiServiceWebClient;
        private final ObjectMapper objectMapper;
        private final LedgerContextPackBuilder ledgerContextPackBuilder;

        // thread nou , cu rollback in caz ca apare ceva
        @Transactional
        public CreateThreadResponse createThread(CreateThreadRequest request) {
                ChatThread thread = ChatThread.builder().orgId(CurrentUser.getOrgId())
                                .createdByUserId(CurrentUser.getUserId())
                                .title(request != null ? request.getTitle() : null).build();

                thread = threadRepository.save(thread);
                log.info("Created chat thread: {}", thread.getId());
                return CreateThreadResponse.builder().threadId(thread.getId()).build();
        }

        // un thread read only
        @Transactional(readOnly = true)
        public ThreadDetailResponse getThread(UUID threadId) {
                ChatThread thread = threadRepository.findByIdAndOrgId(threadId, CurrentUser.getOrgId())
                                .orElseThrow(() -> new IllegalArgumentException("Thread not found: " + threadId));

                List<ChatMessage> messages = messageRepository.findByThreadIdOrderByCreatedAtAsc(threadId);

                List<MessageDto> messageDtos = messages.stream()
                                .map(m -> MessageDto.builder().id(m.getId()).role(m.getRole().name())
                                                .content(m.getContentText()).createdAt(m.getCreatedAt()).build())
                                .toList();

                return ThreadDetailResponse.builder().threadId(thread.getId()).messages(messageDtos).build();
        }

        // trimite mesaj+ primeste respuns
        @Transactional
        public MessageDto sendMessage(UUID threadId, SendMessageRequest request) {
                ChatThread thread = threadRepository.findByIdAndOrgId(threadId, CurrentUser.getOrgId())
                                .orElseThrow(() -> new IllegalArgumentException("Thread not found: " + threadId));

                // inregistreaza mesajul in userMessage
                ChatMessage userMessage = ChatMessage.builder().thread(thread).orgId(CurrentUser.getOrgId())
                                .role(MessageRole.USER).contentText(request.getContent()).build();

                messageRepository.save(userMessage);
                log.debug("Saved user message: {}", userMessage.getId());

                // incarca istoricul conversatiei pt AI multi-turn
                List<ChatMessage> historyMessages = messageRepository.findByThreadIdOrderByCreatedAtAsc(threadId);

                // pt cost (maybe 50)
                int maxTurns = 20;

                List<ChatMessage> recentHistory = historyMessages.size() > maxTurns
                                ? historyMessages.subList(historyMessages.size() - maxTurns, historyMessages.size())
                                : historyMessages;

                List<AiChatTurnDto> aiMessages = recentHistory.stream().map(m -> AiChatTurnDto.builder()
                                .role(m.getRole() == MessageRole.USER ? "user" : "assistant")
                                .content(m.getContentText()).build()).toList();

                // ai request cu ledger conext + conversation history
                String requestId = UUID.randomUUID().toString();
                String correlationId = UUID.randomUUID().toString().substring(0, 8);
                SystemContext systemContext = buildSystemContext();
                FinancialContext financialContext = ledgerContextPackBuilder.build(CurrentUser.getOrgId());

                AiAnalyzeRequest aiRequest = AiAnalyzeRequest.builder()
                                .requestId(requestId)
                                .tenantId(CurrentUser.getOrgId().toString())
                                .userId(CurrentUser.getUserId().toString())
                                .locale("ro-RO")
                                .asOf(Instant.now().atOffset(ZoneOffset.UTC)
                                                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                                .question(request.getContent())
                                .systemContext(systemContext)
                                .financialContext(financialContext)
                                .constraints(AiConstraints.builder()
                                                .advisoryOnly(true)
                                                .allowExternalCalls(false)
                                                .build())
                                .messages(aiMessages)
                                .threadId(threadId.toString())
                                .build();

                // request in python
                log.info("Calling AI service with request_id={}, messages_count={}, financial_context.facts={}",
                                requestId, aiMessages.size(), financialContext.getFacts());
                @SuppressWarnings("unchecked")
                Map<String, Object> aiResponse = aiServiceWebClient.post()
                                .uri("/ai/v1/analyze")
                                .header("X-Request-Id", requestId)
                                .header("X-Correlation-Id", correlationId)
                                .header("Content-Type", "application/json")
                                .bodyValue(aiRequest)
                                .retrieve() // do the requst
                                .bodyToMono(Map.class) // un fel de promise - Future<?>
                                .timeout(java.time.Duration.ofSeconds(90)) // previne blocarea la infinit daca AI-ul nu
                                                                           // raspunde
                                .block(); // trebuie blocat threadul pana vine raspunsul

                if (aiResponse == null) {
                        throw new RuntimeException("AI service returned null response");
                }

                String answerText = (String) aiResponse.getOrDefault("answer_text", "No response from AI");
                String responseRequestId = (String) aiResponse.get("request_id");

                @SuppressWarnings("unchecked")
                Map<String, Object> usage = (Map<String, Object>) aiResponse.get("usage");

                // nu cred
                String model = usage != null ? (String) usage.getOrDefault("model", "unknown") : "unknown";

                Object evidenceObj = aiResponse.get("insights");

                // salveaza mesajul de la ai
                ChatMessage assistantMessage = ChatMessage.builder()
                                .thread(thread)
                                .orgId(CurrentUser.getOrgId())
                                .role(MessageRole.ASSISTANT)
                                .contentText(answerText)
                                .build();
                messageRepository.save(assistantMessage);
                log.debug("Saved assistant message: {}", assistantMessage.getId());

                // metadatele
                String contextHash = computeContextHash(systemContext, financialContext);
                String evidenceJson = serializeEvidence(evidenceObj);

                AiResponseMeta responseMeta = AiResponseMeta.builder()
                                .orgId(CurrentUser.getOrgId())
                                .chatMessage(assistantMessage)
                                .requestId(UUID.fromString(responseRequestId != null ? responseRequestId : requestId))
                                .asOf(OffsetDateTime.now(ZoneOffset.UTC))
                                .model(model)
                                .contextHash(contextHash)
                                .evidence(evidenceJson)
                                .build();
                aiResponseMetaRepository.save(responseMeta);
                log.debug("Saved AI response meta: {}", responseMeta.getId());

                thread.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
                threadRepository.save(thread);

                return MessageDto.builder()
                                .id(assistantMessage.getId())
                                .role(assistantMessage.getRole().name())
                                .content(assistantMessage.getContentText())
                                .createdAt(assistantMessage.getCreatedAt())
                                .build();
        }

        private SystemContext buildSystemContext() {
                LocalDate today = LocalDate.now(ZoneOffset.UTC);
                LocalDate thirtyDaysAgo = today.minusDays(30);

                TimeRange timeRange = TimeRange.builder()
                                .from(thirtyDaysAgo.toString())
                                .to(today.toString())
                                .build();

                CoverageWindow coverage = CoverageWindow.builder()
                                .sourceType("SAFT")
                                .coverageStart(thirtyDaysAgo.toString())
                                .coverageEnd(today.toString())
                                .lastSuccessfulSyncAt(Instant.now().atOffset(ZoneOffset.UTC)
                                                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                                .status("OK")
                                .build();

                return SystemContext.builder()
                                .timeRange(timeRange)
                                .coverage(List.of(coverage))
                                .build();
        }

        // auditabilitate, si reproductibilitate + caching
        private String computeContextHash(SystemContext systemContext, FinancialContext financialContext) {
                try {
                        String systemJson = objectMapper.writeValueAsString(systemContext);
                        String financialJson = objectMapper.writeValueAsString(financialContext);
                        String json = systemJson + financialJson;
                        MessageDigest digest = MessageDigest.getInstance("SHA-256");
                        byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
                        return HexFormat.of().formatHex(hash);
                } catch (JsonProcessingException | NoSuchAlgorithmException e) {
                        log.error("Failed to compute context hash", e);
                        return "error-computing-hash";
                }
        }

        private String serializeEvidence(Object evidence) {
                if (evidence == null) {
                        return "[]";
                }
                try {
                        return objectMapper.writeValueAsString(evidence);
                } catch (JsonProcessingException e) {
                        log.error("Failed to serialize evidence", e);
                        return "[]";
                }
        }
}
