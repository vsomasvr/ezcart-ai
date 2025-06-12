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

public class SoftwareBugAssistant {

    private static final Logger logger = Logger.getLogger(SoftwareBugAssistant.class.getName());

    // --- Define Constants ---
    private static final String MODEL_NAME = "gemini-2.0-flash";

    // ROOT_AGENT needed for ADK Web UI. 
    public static BaseAgent ROOT_AGENT = initAgent();

//    public static void main(String [] args){initAgent();}

    public static BaseAgent initAgent() {
        System.setProperty("GOOGLE_GENAI_USE_VERTEXAI", "TRUE");
        System.setProperty("GOOGLE_CLOUD_PROJECT", "vsoma-demos");
        System.setProperty("GOOGLE_CLOUD_LOCATION", "us-central1");
        System.setProperty("MCP_TOOLBOX_URL", "http://localhost:9090");
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
                    .name("WeatherAssistant")
                    .description("Weather Forecast Summary")
                    .instruction(
                            """
                            You are a skilled expert in providing weather summary.

                            Your general process is as follows:

                            1. **Understand the user's request.** Analyze the user's initial request to understand the goal - for example, "I want weather forecast for new york state. Can you provide a summary?" If you do not understand the request, ask for more information.
                            2. **Identify the appropriate tools.** You will be provided with tools for getting weather forecast for a specific latitude/longitude. You need to do a web search via Google Search to find latitude/longitude values if the request is for a city for example New York City, NY. You can also use tool for alerts based on a state, for example -- new york.
                            3. **Populate and validate the parameters.** Before calling the tools, do some reasoning to make sure that you are populating the tool parameters correctly.
                            4. **Call the tools.** Once the parameters are validated, call the tool with the determined parameters.
                            5. **Analyze the tools' results, and provide insights back to the user.** Return the tools' result in a human-readable format. State which tools you called, if any.
                            6. **Ask the user if they need anything else.**
                """)
                    .tools(allTools)
                    .outputKey("weather_assistant_result")
                    .build();
        } catch (Exception e) {
            logger.info("Error initializing MCP toolset and starting agent " + e.getMessage());
            return null;
        }

    }
}
