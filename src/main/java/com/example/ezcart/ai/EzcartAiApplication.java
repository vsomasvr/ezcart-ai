package com.example.ezcart.ai;

import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static com.example.ezcart.ai.agent.MultiToolAgent.*;

//@SpringBootApplication
public class EzcartAiApplication {

	public static void main(String[] args) {
		SpringApplication.run(EzcartAiApplication.class, args);
	}
	@Bean
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
