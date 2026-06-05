package com.quantumprovenance.shipment.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
public class BlockchainServiceClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${services.blockchain.url:http://localhost:8083}")
    private String blockchainServiceUrl;

    public void registerShipment(String shipmentId, String cid, byte[] merkleRoot) {
        String url = blockchainServiceUrl + "/api/v1/blockchain/register";
        Map<String, Object> request = new HashMap<>();
        request.put("shipmentId", shipmentId);
        request.put("cid", cid);
        request.put("merkleRoot", merkleRoot);

        try {
            restTemplate.postForObject(url, request, Void.class);
            System.out.println("[Client] Registered shipment on blockchain-service");
        } catch (Exception e) {
            System.err.println("BlockchainService offline. Simulating on-chain registry: " + e.getMessage());
        }
    }

    public void updateStatus(String shipmentId, int status, String cid, byte[] merkleRoot) {
        String url = blockchainServiceUrl + "/api/v1/blockchain/update-status";
        Map<String, Object> request = new HashMap<>();
        request.put("shipmentId", shipmentId);
        request.put("status", status);
        request.put("cid", cid);
        request.put("merkleRoot", merkleRoot);

        try {
            restTemplate.postForObject(url, request, Void.class);
            System.out.println("[Client] Updated status on blockchain-service");
        } catch (Exception e) {
            System.err.println("BlockchainService offline. Simulating status update: " + e.getMessage());
        }
    }
}
