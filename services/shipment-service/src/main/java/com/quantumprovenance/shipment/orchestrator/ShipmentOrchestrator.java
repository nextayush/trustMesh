package com.quantumprovenance.shipment.orchestrator;

import com.quantumprovenance.shipment.client.BlockchainServiceClient;
import com.quantumprovenance.shipment.client.CryptoServiceClient;
import com.quantumprovenance.shipment.client.StorageServiceClient;
import com.quantumprovenance.shipment.domain.AuditEvent;
import com.quantumprovenance.shipment.domain.Shipment;
import com.quantumprovenance.shipment.domain.ShipmentStatus;
import com.quantumprovenance.shipment.repository.AuditEventRepository;
import com.quantumprovenance.shipment.repository.ShipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.util.Arrays;

@Service
public class ShipmentOrchestrator {

    private final CryptoServiceClient cryptoClient;
    private final StorageServiceClient storageClient;
    private final BlockchainServiceClient blockchainClient;
    private final ShipmentRepository shipmentRepository;
    private final AuditEventRepository auditRepository;

    public ShipmentOrchestrator(
            CryptoServiceClient cryptoClient,
            StorageServiceClient storageClient,
            BlockchainServiceClient blockchainClient,
            ShipmentRepository shipmentRepository,
            AuditEventRepository auditRepository
    ) {
        this.cryptoClient = cryptoClient;
        this.storageClient = storageClient;
        this.blockchainClient = blockchainClient;
        this.shipmentRepository = shipmentRepository;
        this.auditRepository = auditRepository;
    }

    @Transactional
    public Shipment registerShipment(
            String shipmentId, 
            byte[] manifestBytes, 
            byte[] supplierPublicKey, 
            byte[] supplierPrivateKey,
            String creatorAddress
    ) throws Exception {
        
        // 1. Digital Signature: sign the manifest payload using Dilithium (ML-DSA)
        byte[] signature = cryptoClient.signManifest(manifestBytes, supplierPrivateKey);

        // 2. Encryption: encrypt manifest using AES-256 and ML-KEM key encapsulation
        CryptoServiceClient.CryptoPayload crypto = cryptoClient.encryptManifest(manifestBytes, supplierPublicKey);

        // 3. Merkle Root: build Merkle root over the cipher text and signature
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashCipher = digest.digest(crypto.ciphertext());
        byte[] hashSig = digest.digest(signature);
        
        byte[] concatenated = new byte[64];
        System.arraycopy(hashCipher, 0, concatenated, 0, 32);
        System.arraycopy(hashSig, 0, concatenated, 32, 32);
        byte[] merkleRoot = digest.digest(concatenated);

        // 4. Store: upload the encrypted manifest payload off-chain (MinIO/IPFS)
        String cid = storageClient.storeContent(crypto.ciphertext());

        // 5. Blockchain Anchor: register the record on-chain using web3j
        blockchainClient.registerShipment(shipmentId, cid, merkleRoot);

        // 6. Local State: save details to Postgres database
        Shipment shipment = new Shipment(
                shipmentId,
                cid,
                ShipmentStatus.CREATED,
                creatorAddress,
                creatorAddress,
                merkleRoot
        );
        shipmentRepository.save(shipment);

        // 7. Audit: record local history
        AuditEvent audit = new AuditEvent(
                shipmentId,
                ShipmentStatus.CREATED,
                cid,
                creatorAddress,
                "0x-mock-tx-hash-created",
                merkleRoot
        );
        auditRepository.save(audit);

        System.out.println("[Orchestrator] Completed shipment registration flow for: " + shipmentId);
        return shipment;
    }

    @Transactional
    public Shipment updateShipmentStatus(
            String shipmentId,
            ShipmentStatus newStatus,
            byte[] manifestBytes,
            byte[] supplierPublicKey,
            byte[] supplierPrivateKey,
            String updaterAddress
    ) throws Exception {
        
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new IllegalArgumentException("Shipment not found: " + shipmentId));

        // State Machine validation
        if (newStatus.ordinal() <= shipment.getStatus().ordinal()) {
            throw new IllegalStateException("Invalid state transition from " + shipment.getStatus() + " to " + newStatus);
        }

        // Re-process cryptography and store for the new event metadata
        byte[] signature = cryptoClient.signManifest(manifestBytes, supplierPrivateKey);
        CryptoServiceClient.CryptoPayload crypto = cryptoClient.encryptManifest(manifestBytes, supplierPublicKey);

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashCipher = digest.digest(crypto.ciphertext());
        byte[] hashSig = digest.digest(signature);
        byte[] concatenated = new byte[64];
        System.arraycopy(hashCipher, 0, concatenated, 0, 32);
        System.arraycopy(hashSig, 0, concatenated, 32, 32);
        byte[] merkleRoot = digest.digest(concatenated);

        String cid = storageClient.storeContent(crypto.ciphertext());

        // Update on Blockchain (triggers ABAC checks)
        blockchainClient.updateStatus(shipmentId, newStatus.ordinal(), cid, merkleRoot);

        // Update Local State
        shipment.setStatus(newStatus);
        shipment.setContentIdentifier(cid);
        shipment.setMerkleRoot(merkleRoot);
        shipmentRepository.save(shipment);

        // Record Audit event
        AuditEvent audit = new AuditEvent(
                shipmentId,
                newStatus,
                cid,
                updaterAddress,
                "0x-mock-tx-hash-status-updated",
                merkleRoot
        );
        auditRepository.save(audit);

        System.out.println("[Orchestrator] Completed shipment status update to: " + newStatus + " for: " + shipmentId);
        return shipment;
    }
}
