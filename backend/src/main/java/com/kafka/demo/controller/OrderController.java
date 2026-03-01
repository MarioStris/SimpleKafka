package com.kafka.demo.controller;

import com.kafka.demo.model.OrderEvent;
import com.kafka.demo.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderEvent> createOrder(@RequestBody Map<String, Object> request) {
        String productName = (String) request.get("productName");
        int quantity = (int) request.get("quantity");
        double price = ((Number) request.get("price")).doubleValue();

        OrderEvent event = orderService.createOrder(productName, quantity, price);
        return ResponseEntity.ok(event);
    }
}
