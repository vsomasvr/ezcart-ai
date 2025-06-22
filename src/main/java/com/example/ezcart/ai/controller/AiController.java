package com.example.ezcart.ai.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
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
    private ChatClient chatClient;

    @Autowired
    public AiController(VertexAiGeminiChatModel chatModel, ChatClient chatClient) {
        this.chatModel = chatModel;
        this.chatClient = chatClient;

//        String response = ChatClient.create(chatModel)
//                .prompt("What day is tomorrow?")
//                .tools(new DateTimeTools())
//                .call()
//                .content();
//
//        System.out.println(response);
    }

    @PostMapping("/execute")
    public Map<String, String> execute(@RequestBody String message, Authentication authentication) {
        logger.info("AI endpoint called on behalf of user: " + authentication.getName());
        logger.info("Received message: " + message);
        String response = chatClient
                .prompt(message)
                .call()
                .content();
        return Collections.singletonMap("text", response);
    }
}
