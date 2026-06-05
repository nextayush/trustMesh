package com.quantumprovenance.shipment.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "shipments")
public class Shipment {

    @Id
    @Column(name = "shipment_id", length = 100)
    private String shipmentId;

    @Column(name = "content_identifier", nullable = false)
    private String contentIdentifier;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ShipmentStatus status;

    @Column(name = "current_owner", nullable = false, length = 42)
    private String currentOwner;

    @Column(name = "creator", nullable = false, length = 42)
    private String creator;

    @Column(name = "merkle_root")
    private byte[] merkleRoot;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Shipment() {}

    public Shipment(String shipmentId, String contentIdentifier, ShipmentStatus status, String currentOwner, String creator, byte[] merkleRoot) {
        this.shipmentId = shipmentId;
        this.contentIdentifier = contentIdentifier;
        this.status = status;
        this.currentOwner = currentOwner;
        this.creator = creator;
        this.merkleRoot = merkleRoot;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getShipmentId() { return shipmentId; }
    public void setShipmentId(String shipmentId) { this.shipmentId = shipmentId; }

    public String getContentIdentifier() { return contentIdentifier; }
    public void setContentIdentifier(String contentIdentifier) { this.contentIdentifier = contentIdentifier; }

    public ShipmentStatus getStatus() { return status; }
    public void setStatus(ShipmentStatus status) { this.status = status; }

    public String getCurrentOwner() { return currentOwner; }
    public void setCurrentOwner(String currentOwner) { this.currentOwner = currentOwner; }

    public String getCreator() { return creator; }
    public void setCreator(String creator) { this.creator = creator; }

    public byte[] getMerkleRoot() { return merkleRoot; }
    public void setMerkleRoot(byte[] merkleRoot) { this.merkleRoot = merkleRoot; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
