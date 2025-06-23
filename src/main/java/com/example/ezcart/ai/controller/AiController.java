package com.example.ezcart.ai.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private Logger logger = LoggerFactory.getLogger(AiController.class);

    private final VertexAiGeminiChatModel chatModel;
    private final ChatClient chatClient;

    @Autowired
    public AiController(VertexAiGeminiChatModel chatModel, ChatClient chatClient) {
        this.chatModel = chatModel;
        this.chatClient = chatClient;
    }

    @PostMapping("/execute")
    public Map<String, String> execute(@RequestBody String message, Authentication authentication) {
        logger.info("AI endpoint called on behalf of user: " + authentication.getName());
        logger.info("Received message: " + message);
        String response = chatClient
                .prompt()
                .user(message)
                .advisors(a ->
                        a.param(ChatMemory.CONVERSATION_ID, authentication.getName())
                )
                .call()
                .content();

        return Collections.singletonMap("text", response);
    }
}
