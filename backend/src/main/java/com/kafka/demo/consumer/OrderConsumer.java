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
public class OrderConsumer {

    private final OrderProducer orderProducer;

    @KafkaListener(topics = "order-created", groupId = "order-group")
    public void consume(OrderEvent event) {
        log.info("[OrderConsumer] Received order: orderId={}, product={}", event.getOrderId(), event.getProductName());

        try {
            // Simulate order validation
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        OrderEvent paymentEvent = OrderEvent.builder()
                .orderId(event.getOrderId())
                .productName(event.getProductName())
                .quantity(event.getQuantity())
                .price(event.getPrice())
                .status("PAYMENT_PENDING")
                .timestamp(System.currentTimeMillis())
                .build();

        log.info("[OrderConsumer] Order validated, sending to payment-pending: orderId={}", event.getOrderId());
        orderProducer.send("payment-pending", paymentEvent);
    }
}
