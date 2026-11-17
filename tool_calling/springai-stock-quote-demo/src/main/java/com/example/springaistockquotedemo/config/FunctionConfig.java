package com.example.springaistockquotedemo.config;

import com.example.springaistockquotedemo.service.StockQuoteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FunctionConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public StockQuoteService getStockQuote(StockQuoteService stockQuoteService) {
        return stockQuoteService;
    }
}
