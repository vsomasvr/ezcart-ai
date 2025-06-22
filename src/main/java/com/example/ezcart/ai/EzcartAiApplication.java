package com.example.ezcart.ai;

import com.example.ezcart.ai.config.McpSyncClientExchangeFilterFunction;
import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.modelcontextprotocol.client.McpSyncClient;
import io.reactivex.rxjava3.core.Flowable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;

import static com.example.ezcart.ai.agent.MultiToolAgent.*;

@SpringBootApplication
public class EzcartAiApplication {

	public static void main(String[] args) {
		SpringApplication.run(EzcartAiApplication.class, args);
	}

	@Bean
	ChatClient chatClient(ChatClient.Builder chatClientBuilder, List<McpSyncClient> mcpClients) {
		return chatClientBuilder.defaultToolCallbacks(new SyncMcpToolCallbackProvider(mcpClients)).build();
	}

	/**
	 * Overload Boot's default {@link WebClient.Builder}, so that we can inject an
	 * oauth2-enabled {@link ExchangeFilterFunction} that adds OAuth2 tokens to requests
	 * sent to the MCP server.
	 */
	@Bean
	WebClient.Builder webClientBuilder(McpSyncClientExchangeFilterFunction filterFunction) {
		return WebClient.builder().apply(filterFunction.configuration());
	}

//	@Bean
//	SecurityFilterChain securityFilterChainClient(HttpSecurity http) throws Exception {
//		return http.authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
//				.oauth2Client(Customizer.withDefaults())
//				.csrf(CsrfConfigurer::disable)
//				.build();
//	}

//	@Bean
	CommandLineRunner cli() {
		String mcpServerUrl = System.getenv("MCP_TOOLBOX_URL");
		System.out.println(mcpServerUrl);
		return args -> {
			InMemoryRunner runner = new InMemoryRunner(ROOT_AGENT);

			Session session = runner.sessionService().createSession(NAME, USER_ID).blockingGet();

			try (Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8)) {
				while (true) {
					System.out.print("\nYou > ");
					String userInput = scanner.nextLine();

					if ("quit".equalsIgnoreCase(userInput)) {
						break;
					}

					Content userMsg = Content.fromParts(Part.fromText(userInput));
					Flowable<Event> events = runner.runAsync(USER_ID, session.id(), userMsg);

					System.out.print("\nAgent > ");
					events.blockingForEach(event -> System.out.println(event.stringifyContent()));
				}
			}
		};
	}
}
