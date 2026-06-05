package com.quantumprovenance.storage.service;

import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MinIOStorageAdapterIT {

    private MinIOStorageAdapter adapter;

    @BeforeEach
    public void setup() {
        // Build MinioClient pointing to local running Docker Compose instance
        MinioClient client = MinioClient.builder()
                .endpoint("http://localhost:9000")
                .credentials("minioadmin", "minioadminpassword")
                .build();
        
        adapter = new MinIOStorageAdapter(client, "quantum-provenance-manifests");
    }

    @Test
    public void testStoreAndRetrieveData() {
        byte[] originalContent = "Enterprise-Logistics-Shipment-Metadata-Payload-123456789".getBytes();

        // 1. Store
        String cid = adapter.store(originalContent);
        assertNotNull(cid);
        assertEquals(64, cid.length()); // SHA-256 Hex string is exactly 64 characters

        // 2. Retrieve
        byte[] retrievedContent = adapter.retrieve(cid);
        assertNotNull(retrievedContent);
        assertArrayEquals(originalContent, retrievedContent);
    }
}
