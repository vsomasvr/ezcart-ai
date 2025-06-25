package com.example.ezcart.ai.controller;

import com.example.ezcart.ai.config.ChatClientConfig;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class ProductSummarizationAgent implements Agent {
    private final ChatClient productSearchIdClient;
    private final ChatClient productSummarizationClient;

    public ProductSummarizationAgent(@Qualifier("productSearchIdClient") ChatClient productSearchIdClient,
                                     @Qualifier("productSummarizationClient") ChatClient productSummarizationClient) {
        this.productSearchIdClient = productSearchIdClient;
        this.productSummarizationClient = productSummarizationClient;
    }

    public Map<String, String> execute(String message, String principal) {
        String response = "";
        // @TODO:This fails to call tools sometimes when ChatMemory is used
        String resultWithProductIds = productSearchIdClient
                .prompt()
                .user(message)
//                .advisors(a ->
//                        a.param(ChatMemory.CONVERSATION_ID, principal + "-product-search-id")
//                )
                .call()
                .content();

        ProductSearchResponse productSearchResponse = productSummarizationClient
                .prompt(ChatClientConfig.PRODUCT_SEARCH_ID_FORMAT + "\n Input: " + resultWithProductIds)
                .call()
                .entity(ProductSearchResponse.class);

        if(productSearchResponse != null && !CollectionUtils.isEmpty(productSearchResponse.products())) {
            List<String> inputs = productSearchResponse.products().stream()
                    .map(ProductSearchResult::productId).toList();
            List<String> summaries = parallel("Summarize the product with ProductId", inputs, 5, principal);
            response = String.join("\n", summaries);
        }
        return Collections.singletonMap("text", response);
    }

    public List<String> parallel(String prompt, List<String> inputs, int nWorkers, String CONVERSATION_ID) {
        Assert.notNull(prompt, "Prompt cannot be null");
        Assert.notEmpty(inputs, "Inputs list cannot be empty");
        Assert.isTrue(nWorkers > 0, "Number of workers must be greater than 0");

        ExecutorService executor = Executors.newFixedThreadPool(nWorkers);
        try {
            List<CompletableFuture<String>> futures = inputs.stream()
                    .map(input -> CompletableFuture.supplyAsync(() -> {
                        try {
                            return productSummarizationClient
                                    .prompt(prompt + "\nInput: " + input)
                                    .advisors(a ->
                                            a.param(ChatMemory.CONVERSATION_ID, CONVERSATION_ID)
                                    )
                                    .call()
                                    .content();
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to process input: " + input, e);
                        }
                    }, executor))
                    .collect(Collectors.toList());

            // Wait for all tasks to complete
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                    futures.toArray(CompletableFuture[]::new));
            allFutures.join();

            return futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());

        } finally {
            executor.shutdown();
        }
    }

    public record ProductSearchResponse(
            List<ProductSearchResult> products
    ) {

    }
    public record ProductSearchResult(
            String productId,
            String productName
    ) {
    }
}
