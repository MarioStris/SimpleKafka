package com.kafka.demo.service;

import com.kafka.demo.model.OrderEvent;
import com.kafka.demo.producer.OrderProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderProducer orderProducer;

    public OrderEvent createOrder(String productName, int quantity, double price) {
        OrderEvent event = OrderEvent.builder()
                .orderId(UUID.randomUUID().toString())
                .productName(productName)
                .quantity(quantity)
                .price(price)
                .status("ORDER_CREATED")
                .timestamp(System.currentTimeMillis())
                .build();

        log.info("Creating order: orderId={}, product={}", event.getOrderId(), productName);
        orderProducer.send("order-created", event);
        return event;
    }
}
