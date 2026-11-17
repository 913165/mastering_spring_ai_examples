package com.example.springai_toolsearch_tool_example;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.toolsearch.ToolSearchToolCallingAdvisor;
import org.springframework.ai.session.DefaultSessionService;
import org.springframework.ai.session.InMemorySessionRepository;
import org.springframework.ai.session.SessionService;
import org.springframework.ai.session.advisor.SessionAdvisors;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.ai.tool.toolsearch.index.lucene.LuceneToolIndex;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Scanner;

@Component
@ConditionalOnProperty(prefix = "app.cli", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DeploymentCliRunner implements CommandLineRunner {

    private static final String DIRECT_SESSION = "cli-direct";
    private static final String SEARCH_SESSION = "cli-search";

    private static final String SHARED_RULES = """
            This is one ongoing conversation. Earlier user and assistant messages are included.
            Remember the service name, the environment, and whether the user wants a deploy or a rollback.
            If a detail was already given, do not ask for it again.
            When you have service, environment, and action, call the matching tool immediately.
            Ask only for the one detail that is still missing.
            Reply in one short sentence.
            """;

    private final ChatClient directClient;
    private final ChatClient searchClient;
    private Mode mode = Mode.DIRECT;

    public DeploymentCliRunner(ObjectProvider<ChatClient.Builder> chatClientBuilders, DeploymentTools deploymentTools) {
        SessionService sessionService = DefaultSessionService.builder()
                .sessionRepository(InMemorySessionRepository.builder().build())
                .build();
        var sessionAdvisors = SessionAdvisors.builder(sessionService).build();
        var toolsLogger = new ToolsSentToLlmLogger();

        this.directClient = chatClientBuilders.getObject()
                .defaultTools(deploymentTools)
                .defaultAdvisors(sessionAdvisors)
                .defaultAdvisors(toolsLogger)
                .defaultSystem("""
                        You are a deployment assistant.
                        You can deploy or roll back only by calling one of these tools:
                        deployToDev, deployToUat, deployToProd, rollbackFromDev, rollbackFromUat, rollbackFromProd.
                        dev means Dev, uat means Uat, prod means Prod.
                        """ + SHARED_RULES)
                .build();

        // A ToolSearch advisor is itself a ToolAdvisor, so ChatClient does not also
        // add the default ToolCallingAdvisor. The direct client above has neither,
        // so it keeps the default advisor and sends the full catalog.
        this.searchClient = chatClientBuilders.getObject()
                .defaultTools(deploymentTools)
                .defaultAdvisors(ToolSearchToolCallingAdvisor.builder()
                        .toolIndex(new LuceneToolIndex())
                        .maxResults(2)
                        .build())
                .defaultAdvisors(sessionAdvisors)
                .defaultAdvisors(toolsLogger)
                .defaultSystem("""
                        You are a deployment assistant for deploy and rollback across dev, uat, and prod.
                        The deploy and rollback tools are not in your context until you search for them.
                        Search with the action and the environment, for example "deploy dev" or "rollback prod".
                        """ + SHARED_RULES)
                .build();

        String[] catalog = Arrays.stream(MethodToolCallbackProvider.builder()
                        .toolObjects(deploymentTools)
                        .build()
                        .getToolCallbacks())
                .map(callback -> callback.getToolDefinition().name())
                .sorted()
                .toArray(String[]::new);
        System.out.println("Registered tool catalog: " + catalog.length + " " + Arrays.toString(catalog));
        System.out.println("direct: every model call receives all " + catalog.length + " tools.");
        System.out.println("search: the first model call receives only toolSearchTool; later calls add the matches.");
    }

    @Override
    public void run(String... args) {
        printHelp();
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("\nYou [" + mode.label() + "]: ");
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
                if (switchMode(input)) {
                    continue;
                }

                String reply = activeClient().prompt()
                        .advisors(SessionAdvisors.session(mode.sessionId()))
                        .advisors(spec -> spec.param(ToolsSentToLlmLogger.MODE_KEY, mode.label()))
                        .user(input)
                        .call()
                        .content();
                System.out.println("Assistant: " + reply);
            }
        }
    }

    private boolean switchMode(String input) {
        String normalized = input.toLowerCase();
        if (normalized.equals("mode")) {
            System.out.println("Current mode: " + mode.label() + ". Type 'direct' or 'search'.");
            return true;
        }
        if (normalized.equals("direct") || normalized.equals("mode direct")) {
            mode = Mode.DIRECT;
            System.out.println("Mode: direct. The next request sends the full tool catalog. Session: " + DIRECT_SESSION);
            return true;
        }
        if (normalized.equals("search") || normalized.equals("mode search")) {
            mode = Mode.SEARCH;
            System.out.println("Mode: search. The next request sends toolSearchTool first, then only the tools Lucene returns. Session: " + SEARCH_SESSION);
            return true;
        }
        return false;
    }

    private ChatClient activeClient() {
        return mode == Mode.DIRECT ? directClient : searchClient;
    }

    private static void printHelp() {
        System.out.println("Deployment assistant");
        System.out.println("Modes: direct | search     (each mode keeps its own conversation)");
        System.out.println("Try:   deploy billing-api to dev");
        System.out.println("       rollback orders-service from prod");
        System.out.println("Exit:  exit");
        System.out.println("Watch the [LLM tools] lines. sent= is how many tool definitions that model call received.");
    }

    private enum Mode {
        DIRECT(DIRECT_SESSION),
        SEARCH(SEARCH_SESSION);

        private final String sessionId;

        Mode(String sessionId) {
            this.sessionId = sessionId;
        }

        String sessionId() {
            return sessionId;
        }

        String label() {
            return name().toLowerCase();
        }
    }
}
