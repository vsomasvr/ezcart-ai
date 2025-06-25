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
    @Qualifier("defaultClient")
    public ChatClient defaultClient(ChatClient.Builder chatClientBuilder, List<McpSyncClient> mcpClients) {
        return defaultChatClientBuilder(chatClientBuilder, mcpClients)
                .defaultAdvisors(messageChatMemoryAdvisor())
                .build();
    }

    @Bean
    @Qualifier("agentOrchestratorClient")
    public ChatClient agentOrchestratorClient(ChatClient.Builder chatClientBuilder, List<McpSyncClient> mcpClients) {
        return defaultChatClientBuilder(chatClientBuilder, mcpClients)
                .build();
    }

    @Bean
    @Qualifier("productSearchClient")
    public ChatClient productSearchClient(ChatClient.Builder chatClientBuilder, List<McpSyncClient> mcpClients) {
        return defaultChatClientBuilder(chatClientBuilder, mcpClients)
                .defaultAdvisors(messageChatMemoryAdvisor())
                .defaultSystem(PRODUCT_SEARCH)
                .build();
    }

    @Bean
    @Qualifier("productSearchIdClient")
    public ChatClient productSearchIdClient(ChatClient.Builder chatClientBuilder, List<McpSyncClient> mcpClients) {
        return defaultChatClientBuilder(chatClientBuilder, mcpClients)
//                .defaultAdvisors(simpleLoggerAdvisor(),messageChatMemoryAdvisor())
                .defaultSystem(PRODUCT_SEARCH_ID)
                .build();
    }

    @Bean
    @Qualifier("productSummarizationClient")
    public ChatClient productSummarizationClient(ChatClient.Builder chatClientBuilder, List<McpSyncClient> mcpClients) {
        return defaultChatClientBuilder(chatClientBuilder, mcpClients)
                .defaultAdvisors(messageChatMemoryAdvisor())
                .defaultSystem(PRODUCT_SUMMARIZATION)
                .build();
    }

    private ChatClient.Builder defaultChatClientBuilder(ChatClient.Builder chatClientBuilder, List<McpSyncClient> mcpClients) {
        return chatClientBuilder
                .defaultToolCallbacks(syncMcpToolCallbackProvider(mcpClients))
                .defaultAdvisors(simpleLoggerAdvisor());
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

    @Bean
    public SyncMcpToolCallbackProvider syncMcpToolCallbackProvider(List<McpSyncClient> mcpClients) {
        return new SyncMcpToolCallbackProvider(mcpClients);
    }

    private static final String PRODUCT_SUMMARIZATION = """
             Always use getProductById Get detailed information about a specific product.
            - Extract parameters: productId.
			- Summarize the response concisely with key specs, price and overall value
			- Use an appropriate response template to format the response.
                ...
                If no products are found, respond: "No products found matching your criteria. Please try different filters."
            - Keep responses professional but friendly.
            """;

    public static final String PRODUCT_SEARCH_ID_FORMAT = """
             Return the formatted JSON response as follows:
             
             {
                "products": [
                    {
                        "productId": "Product Id",
                        "productName": "Product Name",
                        "price": "Product Price"
                    },
                    ...
                ]
            }
            
             """;
    private static final String PRODUCT_SEARCH_ID =
            // @TODO: Investogate: Prompt required the first two lines referring to product summarization
            // Otherwise, tool call is not made for summarization.
            // When asked to provide reason for not calling tool, it says "I cannot summarize StreamLine StudioBook 14
            // because that functionality is beyond my capabilities. However, I can search for the product "StreamLine StudioBook 14" using the
            // product search tool and provide you with the product ID, name, and price. Would you like me to do that?"
            """
            You are a skilled expert helping in providing contextual information to summarize a product. 
            You are specialized in searching products and providing Product Name, Product Id, and Price, which is used to summarize the product by the user.
            1. **ALWAYS CALL TOOL: productSearch tool for every request. Explain the reason for not calling the tool, if you didn't**.
            2. **Extract parameters: Identify query, categories, minPrice, maxPrice, manufacturers, ramFilters, processorFilters, and storageFilters.**
			3. **Use the query parameter to search in product names, descriptions and specifications etc.**
            4. **Refine the query to include ONLY 3 to 5 key words that best describe the user's needs.**
            5. **Clarify: If a request is ambiguous or lacks necessary details (e.g., price range for "cheap"), ask the user for more information.**
            6. **Present results: After search, ALWAYS format the response as follows:**

                Sample Presentation Format:

                Here are the products found matching your criteria:
                - [productName: Product Name 1] - [productId: Product Id] - $[Price] 
                - [productName: Product Name 2] - [productId: Product Id] - $[Price] 
                ...
            7. **If no products are found, respond: "No products found matching your criteria. Please try different filters."**
            - Keep responses professional but friendly.
            """;

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
