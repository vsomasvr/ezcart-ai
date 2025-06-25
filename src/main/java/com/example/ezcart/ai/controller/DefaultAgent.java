package com.example.ezcart.ai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

@Service
public class DefaultAgent implements Agent {
    private final ChatClient defaultClient;

    public DefaultAgent(@Qualifier("defaultClient")ChatClient defaultClient) {
        this.defaultClient = defaultClient;
    }

    public Map<String, String> execute(String message, String principal) {
        String response = defaultClient
                .prompt()
                .user(message)
                .advisors(a ->
                        a.param(ChatMemory.CONVERSATION_ID, principal)
                )
                .call()
                .content();
        return Collections.singletonMap("text", response);
    }
}
