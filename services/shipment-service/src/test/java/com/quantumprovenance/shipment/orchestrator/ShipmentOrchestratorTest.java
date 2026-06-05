package com.quantumprovenance.shipment.orchestrator;

import com.quantumprovenance.shipment.client.BlockchainServiceClient;
import com.quantumprovenance.shipment.client.CryptoServiceClient;
import com.quantumprovenance.shipment.client.StorageServiceClient;
import com.quantumprovenance.shipment.domain.AuditEvent;
import com.quantumprovenance.shipment.domain.Shipment;
import com.quantumprovenance.shipment.domain.ShipmentStatus;
import com.quantumprovenance.shipment.repository.AuditEventRepository;
import com.quantumprovenance.shipment.repository.ShipmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class ShipmentOrchestratorTest {

    private CryptoServiceClient cryptoClient;
    private StorageServiceClient storageClient;
    private BlockchainServiceClient blockchainClient;
    private ShipmentRepository shipmentRepository;
    private AuditEventRepository auditRepository;

    private ShipmentOrchestrator orchestrator;

    @BeforeEach
    public void setup() {
        cryptoClient = mock(CryptoServiceClient.class);
        storageClient = mock(StorageServiceClient.class);
        blockchainClient = mock(BlockchainServiceClient.class);
        shipmentRepository = mock(ShipmentRepository.class);
        auditRepository = mock(AuditEventRepository.class);

        orchestrator = new ShipmentOrchestrator(
                cryptoClient,
                storageClient,
                blockchainClient,
                shipmentRepository,
                auditRepository
        );
    }

    @Test
    public void testRegisterShipmentWorkflow() throws Exception {
        String shipmentId = "SHIP-123";
        byte[] manifest = "manifest-test".getBytes();
        byte[] pubKey = "pub-key".getBytes();
        byte[] privKey = "priv-key".getBytes();
        String creator = "0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266";

        // Setup mock behaviours
        when(cryptoClient.signManifest(eq(manifest), eq(privKey))).thenReturn(new byte[64]);
        when(cryptoClient.encryptManifest(eq(manifest), eq(pubKey)))
                .thenReturn(new CryptoServiceClient.CryptoPayload("ciphertext-bytes".getBytes(), new byte[12], new byte[128]));
        when(storageClient.storeContent(any())).thenReturn("QmStorageCID");

        // Execute orchestrator
        Shipment shipment = orchestrator.registerShipment(shipmentId, manifest, pubKey, privKey, creator);

        assertNotNull(shipment);
        assertEquals(shipmentId, shipment.getShipmentId());
        assertEquals("QmStorageCID", shipment.getContentIdentifier());
        assertEquals(ShipmentStatus.CREATED, shipment.getStatus());

        // Verify downstream calls
        verify(cryptoClient, times(1)).signManifest(any(), any());
        verify(cryptoClient, times(1)).encryptManifest(any(), any());
        verify(storageClient, times(1)).storeContent(any());
        verify(blockchainClient, times(1)).registerShipment(eq(shipmentId), eq("QmStorageCID"), any());
        verify(shipmentRepository, times(1)).save(any(Shipment.class));
        verify(auditRepository, times(1)).save(any(AuditEvent.class));
    }

    @Test
    public void testUpdateStatusWorkflow() throws Exception {
        String shipmentId = "SHIP-123";
        byte[] manifest = "manifest-test".getBytes();
        byte[] pubKey = "pub-key".getBytes();
        byte[] privKey = "priv-key".getBytes();
        String updater = "0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266";

        Shipment existing = new Shipment(shipmentId, "oldCID", ShipmentStatus.CREATED, updater, updater, new byte[32]);
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(existing));

        when(cryptoClient.signManifest(any(), any())).thenReturn(new byte[64]);
        when(cryptoClient.encryptManifest(any(), any()))
                .thenReturn(new CryptoServiceClient.CryptoPayload("new-ciphertext".getBytes(), new byte[12], new byte[128]));
        when(storageClient.storeContent(any())).thenReturn("QmNewCID");

        // Execute status update (CREATED -> PACKED)
        Shipment updated = orchestrator.updateShipmentStatus(
                shipmentId,
                ShipmentStatus.PACKED,
                manifest,
                pubKey,
                privKey,
                updater
        );

        assertNotNull(updated);
        assertEquals(ShipmentStatus.PACKED, updated.getStatus());
        assertEquals("QmNewCID", updated.getContentIdentifier());

        // Verify downstream calls
        verify(blockchainClient, times(1)).updateStatus(eq(shipmentId), eq(1), eq("QmNewCID"), any());
        verify(shipmentRepository, times(1)).save(any(Shipment.class));
        verify(auditRepository, times(1)).save(any(AuditEvent.class));
    }
}
