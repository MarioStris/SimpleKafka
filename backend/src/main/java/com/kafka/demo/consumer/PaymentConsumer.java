package com.kafka.demo.consumer;

import com.kafka.demo.model.OrderEvent;
import com.kafka.demo.producer.OrderProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentConsumer {

    private final OrderProducer orderProducer;
    private final Random random = new Random();

    @KafkaListener(topics = "payment-pending", groupId = "payment-group")
    public void consume(OrderEvent event) {
        log.info("[PaymentConsumer] Processing payment: orderId={}, amount={}", event.getOrderId(), event.getPrice());

        try {
            // Simulate payment processing
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        boolean paymentSuccess = random.nextInt(100) < 80; // 80% success rate

        if (paymentSuccess) {
            OrderEvent successEvent = OrderEvent.builder()
                    .orderId(event.getOrderId())
                    .productName(event.getProductName())
                    .quantity(event.getQuantity())
                    .price(event.getPrice())
                    .status("PAYMENT_SUCCESS")
                    .timestamp(System.currentTimeMillis())
                    .build();

            log.info("[PaymentConsumer] Payment successful: orderId={}", event.getOrderId());
            orderProducer.send("payment-success", successEvent);
        } else {
            OrderEvent failedEvent = OrderEvent.builder()
                    .orderId(event.getOrderId())
                    .productName(event.getProductName())
                    .quantity(event.getQuantity())
                    .price(event.getPrice())
                    .status("PAYMENT_FAILED")
                    .timestamp(System.currentTimeMillis())
                    .build();

            log.info("[PaymentConsumer] Payment failed: orderId={}", event.getOrderId());
            orderProducer.send("payment-failed", failedEvent);
        }
    }
}
