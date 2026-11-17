package com.example.springai_toolsearch_tool_example;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.core.Ordered;

import java.util.List;

/**
 * Logs the tool definitions on the request after the tool-calling advisor has
 * chosen them, which is the list the model actually receives.
 */
public class ToolsSentToLlmLogger implements CallAdvisor {

    static final String MODE_KEY = "app.tool-log-mode";

    private static final String ROUND_KEY = ToolsSentToLlmLogger.class.getName() + ".round";

    @Override
    public String getName() {
        return "ToolsSentToLlmLogger";
    }

    @Override
    public int getOrder() {
        // ToolCallingAdvisor is HIGHEST_PRECEDENCE + 300 and selects the tools
        // before it calls the rest of the chain. Session memory is later.
        return Ordered.HIGHEST_PRECEDENCE + 400;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        int round = ((Number) request.context().getOrDefault(ROUND_KEY, 0)).intValue() + 1;
        request.context().put(ROUND_KEY, round);

        Object mode = request.context().getOrDefault(MODE_KEY, "unknown");
        List<String> names = toolNames(request);
        System.out.println("[LLM tools] mode=" + mode + " round=" + round
                + " sent=" + names.size() + " " + names);

        ChatClientResponse response = chain.nextCall(request);
        System.out.println("[LLM tools] mode=" + mode + " round=" + round + " " + tokenLine(response));
        return response;
    }

    private static List<String> toolNames(ChatClientRequest request) {
        if (!(request.prompt().getOptions() instanceof ToolCallingChatOptions options)
                || options.getToolCallbacks() == null) {
            return List.of();
        }
        return options.getToolCallbacks().stream()
                .map(callback -> callback.getToolDefinition().name())
                .sorted()
                .toList();
    }

    private static String tokenLine(ChatClientResponse response) {
        ChatResponse chatResponse = response.chatResponse();
        if (chatResponse == null || chatResponse.getMetadata() == null || chatResponse.getMetadata().getUsage() == null) {
            return "tokens unavailable";
        }
        Usage usage = chatResponse.getMetadata().getUsage();
        return "tokens prompt=" + usage.getPromptTokens()
                + " completion=" + usage.getCompletionTokens()
                + " total=" + usage.getTotalTokens();
    }
}
