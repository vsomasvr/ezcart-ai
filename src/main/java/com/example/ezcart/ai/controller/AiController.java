package com.example.ezcart.ai.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private Logger logger = LoggerFactory.getLogger(AiController.class);

//    private final VertexAiGeminiChatModel chatModel;
    private final ChatClient productSearchClient;
    private final RoutingWorkflow  routerWorkflow;
    private Map<String, ChatClient> supportRoutes;

    @Autowired
    public AiController(@Qualifier("productSearchClient")ChatClient productSearchClient) {
        this.productSearchClient = productSearchClient;
        this.routerWorkflow = new RoutingWorkflow(productSearchClient);
    }

    @PostMapping("/execute")
    public Map<String, String> execute(@RequestBody String message, Authentication authentication) {
        logger.info("AI endpoint called on behalf of user: " + authentication.getName());
        logger.info("Received message: " + message);

        String route = routerWorkflow.determineRoute(message, supportRoutes.keySet());
        logger.info("Selected route: " + route);

        if (supportRoutes.get(route) == null) {
            throw new IllegalArgumentException("Selected route '" + route + "' not found in routes map");
        }

        String response = supportRoutes.get(route)
                .prompt()
                .user(message)
                .advisors(a ->
                        a.param(ChatMemory.CONVERSATION_ID, authentication.getName())
                )
                .call()
                .content();

        return Collections.singletonMap("text", response);
    }

    @PostConstruct
    private void init() {
        supportRoutes = Map.of("product-search", productSearchClient,
                "product-summarization", productSearchClient,
                "product-recommendation-with-spec-and-reviews", productSearchClient,
                "product-summarization-with-reviews", productSearchClient,
                "product-comparison", productSearchClient,
                "add-to-cart", productSearchClient,
                "remove-from-cart", productSearchClient);
    }
}
