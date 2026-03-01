package com.kafka.demo.consumer;

import com.kafka.demo.model.OrderEvent;
import com.kafka.demo.producer.OrderProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShippingConsumer {

    private final OrderProducer orderProducer;

    @KafkaListener(topics = "payment-success", groupId = "shipping-group")
    public void consume(OrderEvent event) {
        log.info("[ShippingConsumer] Preparing shipment: orderId={}, product={}", event.getOrderId(), event.getProductName());

        try {
            // Simulate shipping preparation
            Thread.sleep(2500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        OrderEvent shippedEvent = OrderEvent.builder()
                .orderId(event.getOrderId())
                .productName(event.getProductName())
                .quantity(event.getQuantity())
                .price(event.getPrice())
                .status("ORDER_SHIPPED")
                .timestamp(System.currentTimeMillis())
                .build();

        log.info("[ShippingConsumer] Order shipped: orderId={}", event.getOrderId());
        orderProducer.send("order-shipped", shippedEvent);
    }
}
