package com.example.springaistockquotedemo.controller;

import com.example.springaistockquotedemo.model.StockQuoteRequest;
import com.example.springaistockquotedemo.service.StockQuoteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    private final ChatModel chatModel;

    @Autowired
    public ChatController(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @GetMapping("/chat/{symbol}")
    public String chat(@PathVariable("symbol") String symbol) {
        logger.info("Chat endpoint called for symbol: {}", symbol);
        String userMessage = "What is the current price of " + symbol + "?";
        ToolCallback toolCallback = FunctionToolCallback
                .builder("getStockQuote", new StockQuoteService(null, null))
                .description("Get current stock quote including price, change, and trading information for a given stock symbol")
                .inputType(StockQuoteRequest.class)
                .build();


        String response = ChatClient.create(chatModel)
                .prompt(userMessage).tools(toolCallback)
                .call()
                .content();

        return response;
    }
}
