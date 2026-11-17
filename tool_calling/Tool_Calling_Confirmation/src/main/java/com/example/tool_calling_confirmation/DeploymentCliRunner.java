package com.example.tool_calling_confirmation;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.session.DefaultSessionService;
import org.springframework.ai.session.InMemorySessionRepository;
import org.springframework.ai.session.SessionService;
import org.springframework.ai.session.advisor.SessionAdvisors;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Scanner;

@Component
@ConditionalOnProperty(prefix = "app.cli", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DeploymentCliRunner implements CommandLineRunner {

    private static final String SESSION = "cli-direct";

    private static final String SHARED_RULES = """
            This is one ongoing conversation. Earlier user and assistant messages are included.
            Remember the service name and the environment.
            If a detail was already given, do not ask for it again.
            When you have service, environment, and action, call the matching tool immediately.
            Call deployToProd as soon as a production deploy is requested. Do not ask the user to confirm it.
            If a tool result says the user cancelled the operation, do not call that tool again.
            Ask only for the one detail that is still missing.
            Reply in one short sentence.
            """;

    private final ChatClient chatClient;
    private final ToolConfirmationHandler confirmationHandler;

    public DeploymentCliRunner(ObjectProvider<ChatClient.Builder> chatClientBuilders, DeploymentTools deploymentTools,
                               ToolConfirmationHandler confirmationHandler) {
        this.confirmationHandler = confirmationHandler;
        SessionService sessionService = DefaultSessionService.builder()
                .sessionRepository(InMemorySessionRepository.builder().build())
                .build();
        var sessionAdvisors = SessionAdvisors.builder(sessionService).build();

        this.chatClient = chatClientBuilders.getObject()
                .defaultTools(deploymentTools)
                .defaultAdvisors(sessionAdvisors)
                .defaultSystem("""
                        You are a deployment assistant.
                        You can deploy only by calling one of these tools:
                        deployToDev, deployToUat, deployToProd.
                        dev means Dev, uat means Uat, prod means Prod.
                        """ + SHARED_RULES)
                .build();

        String[] catalog = Arrays.stream(ToolCallbacks.from(deploymentTools))
                .map(callback -> callback.getToolDefinition().name())
                .sorted()
                .toArray(String[]::new);
        System.out.println("Registered tool catalog: " + catalog.length + " " + Arrays.toString(catalog));
        System.out.println("Every model call receives all " + catalog.length + " tools.");
    }

    @Override
    public void run(String... args) {
        printHelp();
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("\nYou: ");
                if (!scanner.hasNextLine()) {
                    break;
                }
                String input = scanner.nextLine().trim();
                if (input.isEmpty()) {
                    continue;
                }
                if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit")) {
                    System.out.println("Bye.");
                    break;
                }

                ChatResponse reply = chatClient.prompt()
                        .advisors(SessionAdvisors.session(SESSION))
                        .user(input)
                        .call()
                        .chatResponse();
                reply = confirmationHandler.handle(chatClient, reply, scanner, SESSION, "direct");
                String answer = reply == null || reply.getResult() == null || reply.getResult().getOutput() == null
                        ? ""
                        : reply.getResult().getOutput().getText();
                System.out.println("Assistant: " + (answer == null ? "" : answer));
            }
        }
    }

    private static void printHelp() {
        System.out.println("Deployment assistant");
        System.out.println("Try:   deploy billing-api to dev");
        System.out.println("       deploy billing-api to prod   (pauses; only y allows it)");
        System.out.println("Exit:  exit");
        System.out.println("Watch the [LLM tools] lines. sent= is how many tool definitions that model call received.");
    }
}
