package io.webetl.controller;

import org.springframework.web.bind.annotation.*;
import io.webetl.service.ChatService;
import io.webetl.client.AIResponse;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public AIResponse chat(@RequestBody ChatRequest request) {
        return chatService.processChat(request.getMessage());
    }

    public static class ChatRequest {
        private String message;
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
} 