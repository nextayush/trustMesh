package com.quantumprovenance.shipment.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shipment_id", nullable = false, length = 100)
    private String shipmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ShipmentStatus status;

    @Column(name = "content_identifier", nullable = false)
    private String contentIdentifier;

    @Column(name = "updater", nullable = false, length = 42)
    private String updater;

    @Column(name = "tx_hash", length = 66)
    private String txHash;

    @Column(name = "merkle_root")
    private byte[] merkleRoot;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public AuditEvent() {}

    public AuditEvent(String shipmentId, ShipmentStatus status, String contentIdentifier, String updater, String txHash, byte[] merkleRoot) {
        this.shipmentId = shipmentId;
        this.status = status;
        this.contentIdentifier = contentIdentifier;
        this.updater = updater;
        this.txHash = txHash;
        this.merkleRoot = merkleRoot;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getShipmentId() { return shipmentId; }
    public void setShipmentId(String shipmentId) { this.shipmentId = shipmentId; }

    public ShipmentStatus getStatus() { return status; }
    public void setStatus(ShipmentStatus status) { this.status = status; }

    public String getContentIdentifier() { return contentIdentifier; }
    public void setContentIdentifier(String contentIdentifier) { this.contentIdentifier = contentIdentifier; }

    public String getUpdater() { return updater; }
    public void setUpdater(String updater) { this.updater = updater; }

    public String getTxHash() { return txHash; }
    public void setTxHash(String txHash) { this.txHash = txHash; }

    public byte[] getMerkleRoot() { return merkleRoot; }
    public void setMerkleRoot(byte[] merkleRoot) { this.merkleRoot = merkleRoot; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
