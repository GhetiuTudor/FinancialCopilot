package com.fincopilot.controller;

import com.fincopilot.dto.chat.*;
import com.fincopilot.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

//controller pt chat
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {
    
    private final ChatService chatService; //injecteaza spring IoC

    //creaza thread nou 
    @PostMapping("/threads")
    public ResponseEntity<CreateThreadResponse> createThread( @RequestBody(required = false) CreateThreadRequest request) {
        CreateThreadResponse response = chatService.createThread(request);
        //return ResponseEntity.status(HttpStatus.CREATED).body(response); ? nu trebuie, oricum spring o sa dea error
        return ResponseEntity.ok(response);
    }

    //ia un thread cu toate mesajele 
    @GetMapping("/threads/{threadId}")
    public ResponseEntity<ThreadDetailResponse> getThread(@PathVariable UUID threadId) {
        ThreadDetailResponse response = chatService.getThread(threadId);
        return ResponseEntity.ok(response);
    }

    //trimite mesaje pe thread
    @PostMapping("/threads/{threadId}/messages")
    public ResponseEntity<MessageDto> sendMessage( @PathVariable UUID threadId, @RequestBody SendMessageRequest request) {
        MessageDto response = chatService.sendMessage(threadId, request);
        return ResponseEntity.ok(response);
        
    }
}
