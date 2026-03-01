package com.kafka.demo.producer;

import com.kafka.demo.model.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderProducer {

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public void send(String topic, OrderEvent event) {
        log.info("Sending to topic [{}]: orderId={}, status={}", topic, event.getOrderId(), event.getStatus());
        kafkaTemplate.send(topic, event.getOrderId(), event);
    }
}
