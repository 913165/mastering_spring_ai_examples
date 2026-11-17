package com.example.tool_calling_confirmation;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallDecision;
import org.springframework.ai.model.tool.ToolCallDecisions;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.session.advisor.SessionAdvisors;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

@Component
public class ToolConfirmationHandler {

    private final ToolCallingManager toolCallingManager;
    private final ToolCallingChatOptions options;

    public ToolConfirmationHandler(ToolCallingManager toolCallingManager, DeploymentTools deploymentTools) {
        this.toolCallingManager = toolCallingManager;
        this.options = ToolCallingChatOptions.builder()
                .toolCallbacks(ToolCallbacks.from(deploymentTools))
                .build();
    }

    public ChatResponse handle(ChatClient chatClient, ChatResponse response, Scanner scanner, String sessionId,
            String mode) {
        while (response != null && !ToolCallDecisions.awaitingDecision(response).isEmpty()) {
            var decisions = confirmToolCalls(response, scanner);
            var result = toolCallingManager.executeToolCalls(new Prompt(List.of(), options), response, decisions);
            var history = result.conversationHistory();

            response = chatClient.prompt()
                    .messages(history.getLast())
                    .advisors(SessionAdvisors.session(sessionId))
                    .call()
                    .chatResponse();
        }
        return response;
    }

    private ToolCallDecisions confirmToolCalls(ChatResponse response, Scanner scanner) {
        var assistantMessage = response.getResult().getOutput();
        var awaiting = ToolCallDecisions.awaitingDecision(response);
        var toolCalls = assistantMessage.getToolCalls();
        var keys = ToolCallDecisions.toolCallKeys(assistantMessage);

        List<ToolCallDecision> decisions = new ArrayList<>();
        for (int i = 0; i < toolCalls.size(); i++) {
            var toolCall = toolCalls.get(i);
            var key = keys.get(i);

            boolean approved = true;
            if (awaiting.contains(key)) {
                System.out.println("\nTool: " + toolCall.name());
                System.out.println("Arguments: " + toolCall.arguments());
                System.out.print("\nAllow? (y/N): ");

                if (!scanner.hasNextLine()) {
                    throw new IllegalStateException("Input closed during confirmation");
                }
                approved = scanner.nextLine().trim().equalsIgnoreCase("y");
            }

            decisions.add(approved
                    ? ToolCallDecision.allow(key)
                    : ToolCallDecision.deny(key,
                            "The user cancelled this operation. Do not call this tool again. Tell the user the operation was cancelled."));
        }
        return ToolCallDecisions.of(decisions.toArray(ToolCallDecision[]::new));
    }
}
