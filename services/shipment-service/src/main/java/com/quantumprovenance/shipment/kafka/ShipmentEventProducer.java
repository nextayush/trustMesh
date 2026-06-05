package com.quantumprovenance.shipment.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ShipmentEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public ShipmentEventProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendShipmentEvent(String shipmentId, String eventType, String detailsJson) {
        String payload = String.format("{\"shipmentId\":\"%s\",\"eventType\":\"%s\",\"details\":%s}", 
                shipmentId, eventType, detailsJson);
        
        kafkaTemplate.send("shipment-events", shipmentId, payload);
        System.out.println("[Kafka] Dispatched event to topic shipment-events for: " + shipmentId);
    }
}
