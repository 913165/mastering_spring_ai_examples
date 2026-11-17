package com.example.spring_ai_two_tool_calling;

import com.example.spring_ai_two_tool_calling.tools.OrderTools;
import org.springframework.ai.session.DefaultSessionService;
import org.springframework.ai.session.InMemorySessionRepository;
import org.springframework.ai.session.SessionService;
import org.springframework.ai.session.advisor.SessionAdvisors;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

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

        String content = chatClient
                .prompt("Cancel order 1042 if it has not shipped yet.")
                .advisors(SessionAdvisors.session(sessionId))
                .call()
                .content();
        System.out.println(content);

        content = chatClient
                .prompt("what was the order number last cancelled.")
                .advisors(SessionAdvisors.session(sessionId))
                .call()
                .content();

        System.out.println(content);
    }
}