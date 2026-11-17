package com.example.springaistockquotedemo.model;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record StockQuoteRequest(
        @JsonPropertyDescription("The stock symbol to get quote for (e.g., IBM, AAPL, MSFT)")
        String symbol
) {
}