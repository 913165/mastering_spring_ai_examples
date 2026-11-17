package com.example.springaistockquotedemo.service;

import com.example.springaistockquotedemo.model.StockQuoteRequest;
import com.example.springaistockquotedemo.model.StockQuoteResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.function.Function;
import java.util.logging.Logger;

@Service
public class StockQuoteService implements Function<StockQuoteRequest, StockQuoteResponse> {

    private static final Logger logger = Logger.getLogger(StockQuoteService.class.getName());
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public StockQuoteService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = (webClientBuilder != null) ? webClientBuilder.build() : WebClient.builder().build();
        this.objectMapper = (objectMapper != null) ? objectMapper : new ObjectMapper();
    }

    @Override
    public StockQuoteResponse apply(StockQuoteRequest stockQuoteRequest) {
        logger.info("apply method called with request: " + stockQuoteRequest);
        try {
            String response = webClient.get()
                    .uri("https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol={symbol}&apikey=demo",
                            stockQuoteRequest.symbol())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            logger.info("Global Quote Response: " + response);
            return toQuoteResponse(response, stockQuoteRequest.symbol());
        } catch (Exception ex) {
            logger.severe("Failed to fetch quote for " + stockQuoteRequest.symbol() + ": " + ex.getMessage());
            return unavailable(stockQuoteRequest.symbol());
        }
    }

    private StockQuoteResponse toQuoteResponse(String response, String symbol) throws Exception {
        JsonNode quote = objectMapper.readTree(response).path("Global Quote");
        if (quote.isMissingNode() || quote.isEmpty()) {
            return unavailable(symbol);
        }
        return new StockQuoteResponse(
                quote.path("01. symbol").asText(symbol),
                quote.path("05. price").asText(),
                quote.path("09. change").asText(),
                quote.path("10. change percent").asText(),
                quote.path("07. latest trading day").asText(),
                true
        );
    }

    private StockQuoteResponse unavailable(String symbol) {
        return new StockQuoteResponse(symbol, null, null, null, null, false);
    }
}
