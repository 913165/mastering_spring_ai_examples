package com.example.two_tool_calling;

import com.example.two_tool_calling.OrderTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.session.DefaultSessionService;
import org.springframework.ai.session.InMemorySessionRepository;
import org.springframework.ai.session.SessionService;
import org.springframework.ai.session.advisor.SessionAdvisors;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Scanner;

@Component
@ConditionalOnProperty(name = "app.order-tools.enabled", havingValue = "true", matchIfMissing = true)
public class OrderToolCallingRunner implements CommandLineRunner {

    private final ChatModel chatModel;

    public OrderToolCallingRunner(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public void run(String... args) {
        SessionService sessionService = DefaultSessionService.builder()
                .sessionRepository(InMemorySessionRepository.builder().build())
                .build();
        OrderTools orderTools = new OrderTools();
        String sessionId = "orders";

        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultTools(orderTools)
                .defaultAdvisors(SessionAdvisors.builder(sessionService).build())
                .build();

        System.out.println("Order assistant. Type a question, or exit to quit.");
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("> ");
            System.out.flush();
            if (!scanner.hasNextLine()) {
                break;
            }
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) {
                break;
            }
            String content = chatClient.prompt()
                    .user(line)
                    .advisors(SessionAdvisors.session(sessionId))
                    .call()
                    .content();
            System.out.println(content);
        }
    }
}
