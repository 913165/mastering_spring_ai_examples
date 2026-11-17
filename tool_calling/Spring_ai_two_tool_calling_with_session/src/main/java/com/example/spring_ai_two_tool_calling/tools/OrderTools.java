package com.example.spring_ai_two_tool_calling.tools;

import org.springframework.ai.tool.annotation.Tool;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

}
