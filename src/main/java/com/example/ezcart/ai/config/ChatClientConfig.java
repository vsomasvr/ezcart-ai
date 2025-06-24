package com.example.ezcart.ai.config;

import io.modelcontextprotocol.client.McpSyncClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ChatClientConfig {

    @Bean
    @Qualifier("productSearchClient")
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder, List<McpSyncClient> mcpClients) {
        return chatClientBuilder
                .defaultToolCallbacks(new SyncMcpToolCallbackProvider(mcpClients))
                .defaultAdvisors(simpleLoggerAdvisor(),messageChatMemoryAdvisor())
                .defaultSystem(PRODUCT_SEARCH)
                .build();
    }

    @Bean
    public MessageChatMemoryAdvisor messageChatMemoryAdvisor() {
       return MessageChatMemoryAdvisor
                .builder(MessageWindowChatMemory
                        .builder()
                        .chatMemoryRepository(new InMemoryChatMemoryRepository())
                        .maxMessages(10)
                        .build())
               .build();
    }
    @Bean
    public SimpleLoggerAdvisor simpleLoggerAdvisor() {
        return new SimpleLoggerAdvisor();
    }

    private static final String PRODUCT_SEARCH = """
            You are a Product Search Agent, specialized in finding products using the productSearch tool.
            Always use productSearch for any product-related queries (find, look up, search, filter).
            - Extract parameters: Identify query, categories, minPrice, maxPrice, manufacturers, ramFilters, processorFilters, and storageFilters.
              
			- Use the query parameter to search in product names, descriptions and specifications etc.
            - Refine the query to include ONLY 3 to 5 key words that best describe the user's needs.
            - Clarify: If a request is ambiguous or lacks necessary details (e.g., price range for "cheap"), ask the user for more information.
            - Present results: After search, format the response as follows:

                Sample Presentation Format:

                Here are the products found matching your criteria:
                - [Product Name 1] - [Manufacturer] - $[Price] ([Key Specs, e.g., 8GB RAM, i7])
                - [Product Name 2] - [Manufacturer] - $[Price] ([Key Specs])
                ...
                If no products are found, respond: "No products found matching your criteria. Please try different filters."
                
            - Keep responses professional but friendly.

            Input: """;
}
