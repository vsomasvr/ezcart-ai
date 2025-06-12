package com.example.ezcart.ai.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LlmAgent;
import com.google.adk.tools.AgentTool;
import com.google.adk.tools.BaseTool;
import com.google.adk.tools.GoogleSearchTool;
import com.google.adk.tools.mcp.McpToolset;
import com.google.adk.tools.mcp.SseServerParameters;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ProductRecommendationEngine {

    private static final Logger logger = Logger.getLogger(ProductRecommendationEngine.class.getName());

    // --- Define Constants ---
    private static final String MODEL_NAME = "gemini-2.0-flash";

    // ROOT_AGENT needed for ADK Web UI.
    public static BaseAgent ROOT_AGENT = initAgent();

//    public static void main(String [] args){initAgent();}

    public static BaseAgent initAgent() {
        // Set up MCP Toolbox connection to Cloud SQL
        try {
//            String mcpServerUrl = "http://localhost:9090";
            String mcpServerUrl = System.getenv("MCP_TOOLBOX_URL");
            if (mcpServerUrl == null || mcpServerUrl.isEmpty()) {
                mcpServerUrl = "http://127.0.0.1:5000/mcp/"; // Fallback URL
                logger.warning("⚠️ MCP_TOOLBOX_URL environment variable not set, using default:" + mcpServerUrl);
            }

            SseServerParameters params = SseServerParameters.builder().url(mcpServerUrl).build();
            logger.info("🕰️ Initializing MCP toolset with params" + params);

            McpToolset.McpToolsAndToolsetResult result = McpToolset.fromServer(params, new ObjectMapper()).get();
            logger.info("⭐ MCP Toolset initialized: " + result.toString());
            if (result.getTools() != null && !result.getTools().isEmpty()) {
                logger.info("⭐ MCP Tools loaded: " + result.getTools().size());
            }
            List<BaseTool> mcpTools = result.getTools().stream()
                    .map(mcpTool -> (BaseTool) mcpTool)
                    .collect(Collectors.toList());
            logger.info("🛠️ MCP TOOLS: " + mcpTools.toString());
            mcpTools.stream().forEach(tool -> {
                System.out.println("Tool: " + tool.name() + ", description: " + tool.description() + ", declaration: " + tool.declaration());
            });

            // Add GoogleSearch tool - Workaround for https://github.com/google/adk-python/issues/134
            LlmAgent googleSearchAgent = LlmAgent.builder()
                    .model("gemini-2.0-flash")
                    .name("google_search_agent")
                    .description("Search Google Search")
                    .instruction("""
                You're a specialist in Google Search
                """)
                    .tools(new GoogleSearchTool()) // Your Google search tool
                    .outputKey("google_search_result")
                    .build();
            AgentTool searchTool = AgentTool.create(googleSearchAgent, false);
            List<BaseTool> allTools = new ArrayList<>(mcpTools);
            allTools.add(searchTool);
            logger.info("🌈 ALL TOOLS: " + allTools.toString());

            return LlmAgent.builder()
                    .model(MODEL_NAME)
                    .name("ProductRecommendationEngine")
                    .description("Product Recommendation Assistant")
                    .instruction(
                            """
                            You are a helpful assistant that recommends products based on user preferences and requirements.

                            Your general process is as follows:

                            1. **Understand the user's needs.** Ask clarifying questions to understand the user's requirements such as:
                               - Budget range
                               - Category (Laptop, Desktop, Tablet, etc.)
                               - Preferred brand (if any)
                               - Intended use (gaming, programming, business, personal etc.)
                               - Desired specifications (CPU, RAM, storage, GPU, etc.)
                               - Screen size preference
                               - Battery life requirements
                               - Any other specific needs

                            2. **Use the searchProducts tool** to search for products that match the user's criteria.
                                - Use the query parameter to search in product names, descriptions and specifications etc.
                                - Refine the query to include ONLY 3 to 5 key words that best describe the user's needs.

                            3. **Analyze the results** and present the best matching products to the user, highlighting:
                               - Key specifications
                               - Price and value for money
                               - Pros and cons of each option 

                            4. **Provide a clear and concise recommendation** for the best product based on their needs and preferences.

                            5. **Ask follow-up questions** to refine the search if needed or if multiple good options are available.

                            Always be polite, patient, and provide accurate information to help the user make an informed decision.
                """)
                    .tools(allTools)
                    .outputKey("product_recommendation_result")
                    .build();
        } catch (Exception e) {
            logger.info("Error initializing MCP toolset and starting agent " + e.getMessage());
            return null;
        }

    }
}
