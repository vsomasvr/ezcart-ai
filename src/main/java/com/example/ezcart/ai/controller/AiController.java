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
    private final Agent productSearchAgent;
    private final Agent productSummarizationAgent;
    private final Agent defaultAgent;
    private final ChatClient agentOrchestratorClient;
    private final RoutingWorkflow  routerWorkflow;
    private Map<String, Agent> supportRoutes;

    @Autowired
    public AiController(Agent productSearchAgent, Agent productSummarizationAgent, Agent defaultAgent,
                        @Qualifier("agentOrchestratorClient")ChatClient agentOrchestratorClient) {
        this.productSearchAgent = productSearchAgent;
        this.productSummarizationAgent = productSummarizationAgent;
        this.defaultAgent = defaultAgent;
        this.agentOrchestratorClient = agentOrchestratorClient;
        this.routerWorkflow = new RoutingWorkflow(agentOrchestratorClient);
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

        Map<String, String> response = supportRoutes.get(route).execute(message, authentication.getName());

        return response;
    }

    @PostConstruct
    private void init() {
        supportRoutes = Map.of("product-search", productSearchAgent,
                "product-summarization", productSummarizationAgent,
                "product-recommendation-with-spec-and-reviews", productSummarizationAgent,
                "product-summarization-with-reviews", productSummarizationAgent,
                "product-comparison", productSummarizationAgent,
                "add-to-cart", productSummarizationAgent,
                "remove-from-cart", productSummarizationAgent,
        "default", defaultAgent);
    }
}
