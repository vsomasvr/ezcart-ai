package com.example.ezcart.ai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class ProductSearchAgent implements Agent {
    private final ChatClient productSearchClient;

    public ProductSearchAgent(@Qualifier("productSearchClient")ChatClient productSearchClient) {
        this.productSearchClient = productSearchClient;
    }

    public Map<String, String> execute(String message, String principal) {
        String response = productSearchClient
                .prompt()
                .user(message)
                .advisors(a ->
                        a.param(ChatMemory.CONVERSATION_ID, principal + "-product-search")
                )
                .call()
                .content();
        return Collections.singletonMap("text", response);
    }
}
