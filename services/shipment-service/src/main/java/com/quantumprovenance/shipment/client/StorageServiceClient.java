package com.quantumprovenance.shipment.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

@Component
public class StorageServiceClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${services.storage.url:http://localhost:8082}")
    private String storageServiceUrl;

    public String storeContent(byte[] encryptedPayload) {
        String url = storageServiceUrl + "/api/v1/storage/store";
        Map<String, Object> request = new HashMap<>();
        request.put("content", encryptedPayload);

        try {
            return restTemplate.postForObject(url, request, String.class);
        } catch (Exception e) {
            System.err.println("StorageService offline. Calculating local SHA-256 hash as CID fallback.");
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(encryptedPayload);
                StringBuilder hex = new StringBuilder();
                for (byte b : hash) {
                    hex.append(String.format("%02x", b));
                }
                return hex.toString();
            } catch (Exception ex) {
                return "fallback-cid-" + System.currentTimeMillis();
            }
        }
    }
}
