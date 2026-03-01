package com.kafka.demo.service;

import com.kafka.demo.model.OrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class SseService {

    private final ConcurrentHashMap<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter register(String orderId) {
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L); // 5 min timeout

        emitters.computeIfAbsent(orderId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(orderId, emitter));
        emitter.onTimeout(() -> removeEmitter(orderId, emitter));
        emitter.onError(e -> removeEmitter(orderId, emitter));

        log.info("SSE client registered for orderId={}", orderId);
        return emitter;
    }

    public void send(String orderId, OrderEvent event) {
        List<SseEmitter> orderEmitters = emitters.get(orderId);
        if (orderEmitters == null) return;

        for (SseEmitter emitter : orderEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("order-event")
                        .data(event));
            } catch (IOException e) {
                removeEmitter(orderId, emitter);
            }
        }
    }

    private void removeEmitter(String orderId, SseEmitter emitter) {
        List<SseEmitter> orderEmitters = emitters.get(orderId);
        if (orderEmitters != null) {
            orderEmitters.remove(emitter);
            if (orderEmitters.isEmpty()) {
                emitters.remove(orderId);
            }
        }
    }
}
