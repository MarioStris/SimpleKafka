package com.kafka.demo.consumer;

import com.kafka.demo.model.OrderEvent;
import com.kafka.demo.service.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final SseService sseService;

    @KafkaListener(
            topics = {"order-created", "payment-pending", "payment-success", "payment-failed", "order-shipped"},
            groupId = "notification-group"
    )
    public void consume(OrderEvent event) {
        log.info("[NotificationConsumer] Pushing SSE: orderId={}, status={}", event.getOrderId(), event.getStatus());
        sseService.send(event.getOrderId(), event);
    }
}
