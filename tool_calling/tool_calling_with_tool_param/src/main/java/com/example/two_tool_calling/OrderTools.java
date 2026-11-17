package com.example.two_tool_calling;

import org.jspecify.annotations.Nullable;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class OrderTools {

    private final Map<String, String> orders = new ConcurrentHashMap<>();

    public OrderTools() {
        orders.put("1042", "processing");
        orders.put("1001", "shipped");
    }

    @Tool(description = "Get the current status of an order by its order id")
    String getOrderStatus(String orderId) {
        String status = orders.get(orderId);
        if (status == null) {
            return "Order " + orderId + " was not found.";
        }
        System.out.println("Tool getOrderStatus(" + orderId + ") -> " + status);
        return "Order " + orderId + " status is " + status + ".";
    }

    @Tool(description = "Cancel an order that has not shipped yet")
    String cancelOrder(String orderId) {
        String status = orders.get(orderId);
        if (status == null) {
            return "Order " + orderId + " was not found.";
        }
        if ("shipped".equals(status) || "delivered".equals(status)) {
            String message = "Order " + orderId + " cannot be cancelled because it has already " + status + ".";
            System.out.println("Tool cancelOrder(" + orderId + ") -> " + message);
            return message;
        }
        if ("cancelled".equals(status)) {
            return "Order " + orderId + " is already cancelled.";
        }
        orders.put(orderId, "cancelled");
        String message = "Order " + orderId + " has been cancelled.";
        System.out.println("Tool cancelOrder(" + orderId + ") -> " + message);
        return message;
    }

    @Tool(description = "List orders. The status filter is optional; omit it to list every order." )
    String listOrders(@ToolParam(required = false, description = "Status to filter by, such as processing, shipped, or cancelled") String status) {
        var matches = orders.entrySet().stream()
                .filter(entry -> status == null || status.isBlank() || entry.getValue().equalsIgnoreCase(status))
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .sorted()
                .collect(Collectors.joining(", "));
        String message = matches.isEmpty() ? "No orders found." : matches;
        System.out.println("Tool listOrders(" + status + ") -> " + message);
        return message;
    }

    @Tool(description = "Look up an order. The customer name is optional and is only used when provided.")
    String lookupOrder(
            @ToolParam(description = "The order id") String orderId,
            @Nullable String customerName) {
        if (orderId == null || orderId.isBlank()) {
            return "An order id is required.";
        }
        String status = orders.get(orderId);
        if (status == null) {
            return "Order " + orderId + " was not found.";
        }
        String message = (customerName == null || customerName.isBlank())
                ? "Order " + orderId + " status is " + status + "."
                : "Order " + orderId + " for " + customerName + " status is " + status + ".";
        System.out.println("Tool lookupOrder(" + orderId + ", " + customerName + ") -> " + message);
        return message;
    }
}
