package com.example.spring_ai_two_tool_calling;

import com.example.spring_ai_two_tool_calling.tools.OrderTools;
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
        String content = ChatClient.create(chatModel)
                .prompt("Cancel order 1042 if it has not shipped yet.")
                .tools(new OrderTools())
                .call()
                .content();

        System.out.println(content);
    }
}