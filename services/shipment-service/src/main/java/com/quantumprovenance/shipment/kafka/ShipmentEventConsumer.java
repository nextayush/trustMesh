package com.quantumprovenance.shipment.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ShipmentEventConsumer {

    @KafkaListener(topics = "shipment-events", groupId = "quantum-provenance-group")
    public void consumeShipmentEvent(String message) {
        System.out.println("[Kafka] Received event payload: " + message);
        // Business logic for push notifications, cache updates, etc. goes here
    }
}
